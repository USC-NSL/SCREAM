package edu.usc.enl.dynamicmeasurement.process.oracle;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.data.trace.FilterTraceMapping;
import edu.usc.enl.dynamicmeasurement.data.trace.TaskTraceReader;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/13/13
 * Time: 6:53 PM
 */
class OneByOneSwitchAssignment extends Assignment {

    public OneByOneSwitchAssignment(OracleResource.TaskRecord taskRecord, double resourceShareStep, double lowThreshold, int epochSize, long startTime, FilterTraceMapping mapping) {
        super(taskRecord, lowThreshold, epochSize, resourceShareStep, startTime, mapping);
    }

    @Override
    public void run() {
        try {
            MultiSwitchTask task = (MultiSwitchTask) taskRecord.getTask();
            System.out.println(task.getName());
            TaskTraceReader traceInputForTask = null;
            Map<MonitorPoint, Map<Integer, Integer>> monitorPointSatisfied = new HashMap<>();
            //for each switch
            List<MonitorPoint> monitorPoints = new ArrayList<>(task.getMonitorPoints());
            Collections.shuffle(monitorPoints, new Random(93223939768l));
            for (MonitorPoint otherMonitorPoints : task.getMonitorPoints()) {
                task.getViewFor(otherMonitorPoints).setResourceShare((int) (otherMonitorPoints.getCapacity() * resourceShareStep));
            }
            for (MonitorPoint monitorPoint : monitorPoints) {
                traceInputForTask = runForMonitorPoint(task, traceInputForTask, monitorPointSatisfied, monitorPoint);
            }

            //now test if it works
            traceInputForTask.reset();
            for (int step = taskRecord.getStartEpoch(); step < taskRecord.getRemoveEpoch(); step++) {
                task.setStep(step);
                for (MonitorPoint otherMonitorPoints : task.getMonitorPoints()) {
                    Map<Integer, Integer> stepResourceMap = monitorPointSatisfied.get(otherMonitorPoints);
                    Integer resource = stepResourceMap.get(step);
                    if (resource == null) {
                        throw new RuntimeException("Could not find a satisfied value resource for task " + task + " on step " + step);
                    }
                    task.getViewFor(otherMonitorPoints).setResourceShare(resource);
                }
                runTask(task, step);
                double globalAccuracy = task.getGlobalAccuracy();
                System.out.println(step + ":" + globalAccuracy);
                if (globalAccuracy < lowThreshold) {
                    System.out.println("Task " + task + " is not satisfied on step " + step);
                }
            }


            task.finish(new FinishPacket(taskRecord.getRemoveEpoch() * epochSize + startTime));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private TaskTraceReader runForMonitorPoint(MultiSwitchTask task, TaskTraceReader traceInputForTask,
                                               Map<MonitorPoint, Map<Integer, Integer>> monitorPointSatisfied, MonitorPoint monitorPoint) throws IOException {
        int resourceStep = (int) (monitorPoint.getCapacity() * resourceShareStep);
        int resource = 0;
        boolean allTrue = false;
        Map<Integer, Boolean> satisfiedHistory = new HashMap<>();
        Map<Integer, Boolean> satisfied = new HashMap<>();
        Map<Integer, Integer> satisfiedResourceHistory = new TreeMap<>();
        monitorPointSatisfied.put(monitorPoint, satisfiedResourceHistory);
        //for each resource amount
        while (resource <= monitorPoint.getCapacity() && !allTrue) {
            if (traceInputForTask == null) {
                traceInputForTask = mapping.getInputTrace(task.getFilter()).getTaskTraceReader(task, taskRecord.getStartEpoch(), true);
            } else {
                traceInputForTask.reset();
            }
            task.setTraceReader(traceInputForTask);
            satisfied.clear();
            resource += resourceStep;
            task.getViewFor(monitorPoint).setResourceShare(resource);

            for (int step = taskRecord.getStartEpoch(); step < taskRecord.getRemoveEpoch(); step++) {
                //read trace, report, getlocalaccuracy, updatestructure
                runTask(task, step);

                double accuracy = task.getViewFor(monitorPoint).getAccuracy2();
                System.out.println("task " + task + " MonitorPoint " + monitorPoint.getIntId() + " resource " + resource + " step " + step + " accuracy " + accuracy);
                //gather when a task is ok
                satisfied.put(step, accuracy >= lowThreshold);

            }
            //for each epoch select the resource that the task local accuracy was ok

            allTrue = updateStats(resource, satisfiedHistory, satisfied, satisfiedResourceHistory);
        }
        //write result
        try (PrintWriter pw = new PrintWriter(task.getOutputFolder() + "/" + monitorPoint.getIntId() + ".csv")) {
            for (Map.Entry<Integer, Integer> entry : satisfiedResourceHistory.entrySet()) {
                pw.println(entry.getKey() + "," + entry.getValue());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return traceInputForTask;
    }

    private boolean updateStats(int resource, Map<Integer, Boolean> satisfiedHistory, Map<Integer, Boolean> satisfied, Map<Integer, Integer> satisfiedResourceHistory) {
        boolean output = true;
        for (Map.Entry<Integer, Boolean> entry : satisfied.entrySet()) {
            Boolean sh = satisfiedHistory.get(entry.getKey());
            Boolean s = entry.getValue();
            if (sh == null) {
                satisfiedHistory.put(entry.getKey(), s);
                if (s) {
                    satisfiedResourceHistory.put(entry.getKey(), resource);
                }
            } else if (!sh && s) {
                satisfiedResourceHistory.put(entry.getKey(), resource);
                satisfiedHistory.put(entry.getKey(), true);
            }
            output &= satisfiedHistory.get(entry.getKey());
        }
        return output;
    }

}
