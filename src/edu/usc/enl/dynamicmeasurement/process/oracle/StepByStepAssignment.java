package edu.usc.enl.dynamicmeasurement.process.oracle;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.data.DataPacket;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.data.trace.FilterTraceMapping;
import edu.usc.enl.dynamicmeasurement.data.trace.InputTrace;
import edu.usc.enl.dynamicmeasurement.data.trace.TaskTraceReader;
import edu.usc.enl.dynamicmeasurement.data.trace.TaskTraceReaderInterface;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/13/13
 * Time: 6:53 PM
 */
class StepByStepAssignment extends Assignment {
    private final int minResource;
    private final int warningArea;
    private Map<Task2, InputTrace> taskInputTraceMap;

    public StepByStepAssignment(OracleResource.TaskRecord taskRecord, double resourceShareStep, double lowThreshold, int epochSize,
                                long startTime, FilterTraceMapping mapping,
                                int minResource, Map<Task2, InputTrace> taskInputTraceMap, int warningArea) {
        super(taskRecord, lowThreshold, epochSize, resourceShareStep, startTime, mapping);
        this.minResource = minResource;
        this.taskInputTraceMap = taskInputTraceMap;
        this.warningArea = warningArea;
    }

    @Override
    public void run() {
        double maxCapacityPerTask = 1;
        final MultiSwitchTask task = (MultiSwitchTask) taskRecord.getTask();
        MonitorPointComparator monitorPointComparator = new MonitorPointComparator(task);
        System.out.println("Start " + task.getName());
        TaskTraceReader traceInputForTask = null;
        //for each switch
        List<MonitorPoint> monitorPoints = new ArrayList<>(task.getMonitorPoints());
        Random random = new Random(93223939768l);
        boolean log = false;

        long time = System.currentTimeMillis();
        int attempt = 0;
        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(task.getOutputFolder() + "/share.csv")))) {
            RepeatingTraceReader repeatingTraceReader = new RepeatingTraceReader(taskInputTraceMap.get(task).getTaskTraceReader(task, taskRecord.getStartEpoch(), false));
            task.setTraceReader(repeatingTraceReader);
            for (int step = taskRecord.getStartEpoch(); step < taskRecord.getRemoveEpoch(); step++) {
                for (MonitorPoint otherMonitorPoints : monitorPoints) {
                    task.getViewFor(otherMonitorPoints).setResourceShare(minResource);
                }
                if (log) System.out.println(step + " -----------------------------------------------------------");
                task.setStep(step);
                repeatingTraceReader.emptyCache();
                runTask(task, step);
                double globalAccuracy = task.getGlobalAccuracy();
                double lastAccuracy = -1;
                int equalAccuracy = 0;
                if (log) System.out.println("+++++++++ " + globalAccuracy);

                while (globalAccuracy < lowThreshold) {
                    attempt++;
                    if (globalAccuracy == lastAccuracy) {
                        equalAccuracy++;
                    } else {
                        equalAccuracy = 0;
                    }
                    Collections.shuffle(monitorPoints, random);
                    Collections.sort(monitorPoints, monitorPointComparator);
                    //find minimum monitor point
                    boolean couldIncrease = false;

                    for (MonitorPoint monitorPoint : monitorPoints) {
                        int resourceShare = task.getViewFor(monitorPoint).getResourceShare();
                        int capacity = monitorPoint.getCapacity();
                        if (log) System.out.println(monitorPoint.getIntId() + " " + resourceShare);
                        if (resourceShare >= warningArea && equalAccuracy >= 3) {
                            System.out.println("warning task " + task + " at step " + step + " with accuracy " + globalAccuracy + " used " + resourceShare);
                            break;
                        }
                        if (resourceShare < capacity * maxCapacityPerTask) {
                            couldIncrease = true;
                            if (resourceShare == minResource) {
                                resourceShare = 0;
                            }
                            task.getViewFor(monitorPoint).setResourceShare((int) Math.min(capacity, resourceShare + resourceShareStep * capacity));
                            break;
                        }
                    }
                    if (!couldIncrease) {
                        System.out.println("Could not satisfy task " + task + " at step " + step + " with accuracy " + globalAccuracy);
                        for (MonitorPoint monitorPoint : monitorPoints) {
                            task.getViewFor(monitorPoint).setResourceShare(minResource);
                        }
//                            repeatingTraceReader.startFromCache();
//                            runTask(task, step);
//                            globalAccuracy = task.getGlobalAccuracy();
                        break;
                    }
                    lastAccuracy = globalAccuracy;
                    repeatingTraceReader.startFromCache();
                    runTask(task, step);
                    globalAccuracy = task.getGlobalAccuracy();
                    if (log) System.out.println("+++++++++ " + globalAccuracy);
                }

                for (MonitorPoint monitorPoint : monitorPoints) {
                    pw.println(step + "," + monitorPoint.getIntId() + "," + task.getViewFor(monitorPoint).getResourceShare());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Finish " + task.getName() + ":" + (System.currentTimeMillis() - time) / 1000.0 + "," + attempt / (taskRecord.getRemoveEpoch() - taskRecord.getStartEpoch()));
        task.finish(new FinishPacket(taskRecord.getRemoveEpoch() * epochSize + startTime));
    }

    private class RepeatingTraceReader implements TaskTraceReaderInterface {
        private final TaskTraceReader traceReader;
        private LinkedList<DataPacket> cache = new LinkedList<>();
        private Iterator<DataPacket> cacheIterator;

        private RepeatingTraceReader(TaskTraceReader traceReader) {
            this.traceReader = traceReader;
        }

        @Override
        public DataPacket getNextPacket(DataPacket p) throws DataPacket.PacketParseException, IOException {
            if (cacheIterator != null) {
                if (cacheIterator.hasNext()) {
                    return cacheIterator.next();
                } else {
                    return null;
                }
            } else {
                DataPacket nextPacket = traceReader.getNextPacket(null);
                cache.add(nextPacket);
                return nextPacket;
            }
        }

        public void startFromCache() {
            cacheIterator = cache.iterator();
        }

        @Override
        public void finish() {
            traceReader.finish();
        }

        @Override
        public void keepForNext(DataPacket p) {
            traceReader.keepForNext(p);
            cache.removeLast();
        }

        public void emptyCache() {
            cache.clear();
            cacheIterator = null;
        }
    }

    private class MonitorPointComparator implements Comparator<MonitorPoint> {
        private final MultiSwitchTask task;

        public MonitorPointComparator(MultiSwitchTask task) {
            this.task = task;
        }

        @Override
        public int compare(MonitorPoint o1, MonitorPoint o2) {
            return Double.compare(task.getViewFor(o1).getAccuracy2(), task.getViewFor(o2).getAccuracy2());
        }
    }
}
