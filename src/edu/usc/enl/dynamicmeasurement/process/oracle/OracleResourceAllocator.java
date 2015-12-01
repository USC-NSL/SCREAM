package edu.usc.enl.dynamicmeasurement.process.oracle;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.resourceallocation.AllocationTaskView;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.resourceallocation.MultiTaskResourceControl;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/14/13
 * Time: 4:47 PM
 */
public class OracleResourceAllocator extends MultiTaskResourceControl {
    private MonitorPointData monitorPointData;
    private Map<AllocationTaskView, TaskRecord> tasks = new HashMap<>();
    private int step;

    public OracleResourceAllocator(Element element, MonitorPoint monitorPoint) {
        monitorPointData = new MonitorPointData(monitorPoint);
        step = 0;
    }

    @Override
    public void allocate() {
        for (TaskRecord task : tasks.values()) {
            task.allocate(step + 1); //allocate for next step
        }

        step++;
    }

    @Override
    public boolean addTask(AllocationTaskView task) {
        Map<Integer, Integer> profile = getProfile(task.getTask().getOutputFolder());
        boolean added = monitorPointData.add(profile);
        if (added) {
            TaskRecord taskRecord = new TaskRecord(task, profile);
            tasks.put(task, taskRecord);
            taskRecord.allocate(step);
        }
        return added;
    }

    private Map<Integer, Integer> getProfile(String outputFolder) {
        Map<Integer, Integer> output = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(outputFolder + "/share.csv"))) {
            while (br.ready()) {
                String line = br.readLine();
                String[] split = line.split(",");
                if (split[1].equals(monitorPointData.getMonitorPoint().getIntId() + "")) {
                    output.put(Integer.parseInt(split[0]), Integer.parseInt(split[2]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    @Override
    public void removeTask(AllocationTaskView task) {
        if (tasks.containsKey(task)) {
            monitorPointData.remove(tasks.get(task).getProfile(), step);
            tasks.remove(task);
        }
    }

    private static class MonitorPointData {
        private final MonitorPoint monitorPoint;
        private final Map<Integer, Integer> remainingCapacityProfile;

        private MonitorPointData(MonitorPoint monitorPoint) {
            this.monitorPoint = monitorPoint;
            remainingCapacityProfile = new HashMap<>();
        }

        @Override
        public String toString() {
            return "MonitorPointData{" +
                    "monitorPoint=" + monitorPoint +
                    '}';
        }

        public MonitorPoint getMonitorPoint() {
            return monitorPoint;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MonitorPointData that = (MonitorPointData) o;

            if (monitorPoint != null ? !monitorPoint.equals(that.monitorPoint) : that.monitorPoint != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return monitorPoint != null ? monitorPoint.hashCode() : 0;
        }

        public boolean add(Map<Integer, Integer> profile) {
            for (Map.Entry<Integer, Integer> entry : profile.entrySet()) {
                Integer time = entry.getKey();
                Integer remaining = remainingCapacityProfile.get(time);
                if (remaining == null) {
                    remaining = monitorPoint.getCapacity();
                }
                remaining -= entry.getValue();
                if (remaining < 0) {
                    //revert up to now
                    for (Map.Entry<Integer, Integer> entry2 : profile.entrySet()) {
                        Integer time2 = entry2.getKey();
                        if (time2.equals(time)) {
                            break;
                        }
                        remainingCapacityProfile.put(time2, remainingCapacityProfile.get(time2) + entry2.getValue());
                    }
                    return false;
                }
                remainingCapacityProfile.put(time, remaining);
            }

            return true;
        }

        public void remove(Map<Integer, Integer> profile, int step) {
            for (Map.Entry<Integer, Integer> entry : profile.entrySet()) {
                Integer time = entry.getKey();
                if (time >= step) {
                    remainingCapacityProfile.put(time, remainingCapacityProfile.get(time) + entry.getValue());
                }
            }
        }
    }

    private static class TaskRecord {
        private final AllocationTaskView taskView;
        private final Map<Integer, Integer> profile;

        private TaskRecord(AllocationTaskView taskView, Map<Integer, Integer> profile) {
            this.taskView = taskView;
            this.profile = profile;
        }

        public void allocate(int step) {
            Integer c = profile.get(step);
            if (c != null) {//the task will be removed
                taskView.setResourceShare(c);
            }
        }

        public Map<Integer, Integer> getProfile() {
            return profile;
        }

        @Override
        public String toString() {
            return "TaskRecord{" +
                    "taskView=" + taskView +
                    '}';
        }

    }
}
