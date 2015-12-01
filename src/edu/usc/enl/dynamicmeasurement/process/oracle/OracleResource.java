package edu.usc.enl.dynamicmeasurement.process.oracle;

import edu.usc.enl.dynamicmeasurement.algorithms.taskhandler.TaskHandler;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.aggregator.AccuracyAggregator;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.aggregator.MovingAverageAccuracyAggregatorImpl;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.data.DataPacket;
import edu.usc.enl.dynamicmeasurement.data.trace.FilterTraceMapping;
import edu.usc.enl.dynamicmeasurement.data.trace.InputTrace;
import edu.usc.enl.dynamicmeasurement.model.event.*;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.process.EpochPacket;
import edu.usc.enl.dynamicmeasurement.util.SimulationConfiguration;
import edu.usc.enl.dynamicmeasurement.util.Util;
import edu.usc.enl.dynamicmeasurement.util.multithread.MultiThread;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/12/13
 * Time: 7:59 AM
 */
public class OracleResource {
    public static void main(String[] args) throws Exception {
        String filename = args[0];
        double lowThreshold = Double.parseDouble(args[1]);
        int warningArea = Integer.parseInt(args[2]);
        int minTaskId = 0;
        int maxTaskId = Integer.MAX_VALUE;
        if (args.length > 3) {
            minTaskId = Integer.parseInt(args[3]);
            maxTaskId = Integer.parseInt(args[4]);
        }

        //load information
        File file = new File(filename);
        if (file.isDirectory()) {
            filename = file.getAbsolutePath() + "/config.xml";
        }
        ConfigReader configReader = new ConfigReader();
        configReader.read(filename);


        new OracleResource().run(configReader, lowThreshold, warningArea, minTaskId, maxTaskId);
    }

    private void run(ConfigReader configReader, double lowThreshold, int warningArea, int minTaskId, int maxTaskId) throws Exception {
        final double resourceShareStep = 1.0 / 64;
        final int minResource = 128;
        SimulationConfiguration simulationConfiguration = Util.getSimulationConfiguration();
        final int epochSize = (int) simulationConfiguration.getEpoch();
        final long startTime = simulationConfiguration.getStartTime();

        final FilterTraceMapping mapping = new FilterTraceMapping();
        Map<Task2, InputTrace> taskInputTraceMap = new HashMap<>();
        List<TaskRecord> tasks = getTasks(configReader.getEvents(), epochSize, startTime, mapping, taskInputTraceMap, minTaskId, maxTaskId);
        MultiThread multiThread = new MultiThread(Util.getSimulationConfiguration().getThreads());

        //TODO remove the following
        for (Iterator<TaskRecord> iterator = tasks.iterator(); iterator.hasNext(); ) {
            TaskRecord taskRecord = iterator.next();
            int id = Integer.parseInt(taskRecord.getTask().getName());
            if (id < minTaskId || id > maxTaskId) {
                iterator.remove();
                taskInputTraceMap.remove(taskRecord.getTask());
                taskRecord.getTask().finish(null);
            }
        }
        System.gc();

        //for each task
        for (final TaskRecord taskRecord : tasks) {
            int id = Integer.parseInt(taskRecord.getTask().getName());
            multiThread.offer(new StepByStepAssignment(taskRecord, resourceShareStep, lowThreshold, epochSize, startTime,
                    mapping, minResource, taskInputTraceMap, warningArea));
        }
        multiThread.runJoin();
        multiThread.finishThreads();
    }

    private List<TaskRecord> getTasks(LinkedList<Event> events, int epochSize, long startTime, FilterTraceMapping filterTraceMapping,
                                      Map<Task2, InputTrace> taskInputTraceMap, int minId, int maxId) throws Exception {

        GatherTasksTaskHandler taskHandler = new GatherTasksTaskHandler();
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext(); ) {
            Event event = iterator.next();
            event.setEpoch(event.getEpoch() / epochSize);

            if (event instanceof TaskEvent) {
                ((TaskEvent) event).setHandler(taskHandler);
                if (event instanceof AddTraceTaskEvent) {
                    int id = Integer.parseInt(Util.getChildrenProperties(event.getElement(), "Property").values().iterator().next().getAttribute(ConfigReader.PROPERTY_NAME));
                    if (id < minId || id > maxId) {
                        iterator.remove();
                        continue;
                    }
                    ((AddTraceTaskEvent) event).setMapping(filterTraceMapping);
                    ((OracleAddTraceTaskEvent) event).setInputTraceMap(taskInputTraceMap);
                }
                if (event instanceof RemoveTaskEvent) {
                    int id = Integer.parseInt(Util.getChildrenProperties(event.getElement(), "Property").values().iterator().next().getAttribute(ConfigReader.PROPERTY_VALUE));
                    if (id < minId || id > maxId) {
                        iterator.remove();
                        continue;
                    }
                }
            } else if (event instanceof TransformEvent) {
                ((TransformEvent) event).setHandler(taskHandler);
            } else if (event instanceof AddTrafficEvent) {
                ((AddTrafficEvent) event).setHandler(filterTraceMapping);
                event.run();
            }
        }
        EventRunner eventRunner = new EventRunner(taskHandler, events);

        int step = 0;

        while (!eventRunner.isEmpty()) {
            eventRunner.forceStep(new EpochPacket(step * epochSize + startTime, step++));
//            System.gc();
            taskHandler.writeProfiles();
            Util.flushAllControlledWriters();
        }
        Map<String, TaskRecord> taskRecords = taskHandler.getTaskRecords();
        List<TaskRecord> output = new ArrayList<>(taskRecords.values());
        Collections.sort(output, new Comparator<TaskRecord>() {
            @Override
            public int compare(TaskRecord o1, TaskRecord o2) {
                return o1.getStartEpoch() - o2.getStartEpoch();
            }
        });

        return output;
    }

    static class TaskRecord {
        private Task2 task;
        private int startEpoch;
        private int removeEpoch;

        private TaskRecord(Task2 task) {
            this.task = task;
        }

        public Task2 getTask() {
            return task;
        }

        public void setTask(Task2 task) {
            this.task = task;
        }

        public int getStartEpoch() {
            return startEpoch;
        }

        public void setStartEpoch(int startEpoch) {
            this.startEpoch = startEpoch;
        }

        public int getRemoveEpoch() {
            return removeEpoch;
        }

        public void setRemoveEpoch(int removeEpoch) {
            this.removeEpoch = removeEpoch;
        }
    }

    private static class GatherTasksTaskHandler extends TaskHandler {
        private Map<String, TaskRecord> taskRecords = new HashMap<>();
        private Collection<Task2> tasks = new ArrayList<>();

        public Map<String, TaskRecord> getTaskRecords() {
            return taskRecords;
        }

        @Override
        public Collection<? extends Task2> getTasks() {
            return tasks;
        }

        @Override
        public void addTask(Task2 task2, int step) {
            MultiSwitchTask task = (MultiSwitchTask) task2;
            super.addTask(task, step);
            TaskRecord taskRecord = new TaskRecord(task);
            taskRecords.put(task.getName(), taskRecord);
            taskRecord.setStartEpoch(step);
            tasks.add(task);
            task.setAccuracyAggregator(new MovingAverageAccuracyAggregatorImpl(1));
            for (MonitorPoint monitorPoint : task.getMonitorPoints()) {
                AccuracyAggregator accuracyAggregator = new MovingAverageAccuracyAggregatorImpl(1);
                task.setAccuracyAggregator(monitorPoint, accuracyAggregator);
            }
        }

        @Override
        public void removeTask(String taskName, int step) {
            TaskRecord taskRecord = taskRecords.get(taskName);
            taskRecord.setRemoveEpoch(step);
            tasks.remove(taskRecord.getTask());
        }

        @Override
        protected void step(EpochPacket p) {
        }

        @Override
        public void writeLog(int step) {

        }

        @Override
        protected void process2(DataPacket p) {

        }
    }
}
