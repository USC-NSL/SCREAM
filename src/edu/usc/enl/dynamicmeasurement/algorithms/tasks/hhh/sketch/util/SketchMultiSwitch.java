package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/9/2014
 * Time: 5:06 PM
 */
public class SketchMultiSwitch {
    protected Map<MonitorPoint, MonitorPointData> monitorPoints;

    public SketchMultiSwitch(Map<MonitorPoint, AbstractHierarchicalCountMinSketch> map) {
        monitorPoints = new HashMap<>(map.size(), 1);
        for (Map.Entry<MonitorPoint, AbstractHierarchicalCountMinSketch> entry : map.entrySet()) {
            monitorPoints.put(entry.getKey(), new MonitorPointData(entry.getValue(), entry.getKey()));
        }
    }

    public void getUsedResources(Map<MonitorPoint, Integer> resource) {
        for (Map.Entry<MonitorPoint, Integer> entry : resource.entrySet()) {
            entry.setValue(monitorPoints.get(entry.getKey()).getSketch().getUsedResourceShare());
        }
    }

    public void prepare() {
        apply(monitorPoint -> monitorPoint.getSketch().prepareToFindHHH());
    }

    public long getSum() {
        return monitorPoints.values().stream().mapToLong(m -> m.getSketch().getSum()).sum();
    }

    public void setMonitorPointsItems(Map<Integer, List<HHHOutput>> result) {
        for (Map.Entry<Integer, List<HHHOutput>> entry : result.entrySet()) {
            for (MonitorPointData monitorPoint : monitorPoints.values()) {
                Set<HHHOutput> mHHOutput = new HashSet<>();
                for (HHHOutput hhhOutput : entry.getValue()) {
                    if (monitorPoint.getMonitorPoint().hasDataFrom(hhhOutput.getWildcardPattern())) {
                        mHHOutput.add(hhhOutput);
                    }
                }
                if (mHHOutput.size() > 0) {
                    monitorPoint.getLevelHHH().put(entry.getKey(), mHHOutput);
                }
            }
        }
    }

    public void setCapacityShare(Map<MonitorPoint, Integer> resource) {
        for (Map.Entry<MonitorPoint, Integer> entry : resource.entrySet()) {
            monitorPoints.get(entry.getKey()).getSketch().setCapacityShare(entry.getValue());
        }
    }

    public void setStep(int step) {
        for (MonitorPointData monitorPointData : monitorPoints.values()) {
            monitorPointData.getSketch().setStep(step);
        }
    }

    public void apply(Consumer<MonitorPointData> c) {
        for (MonitorPointData monitorPointData : monitorPoints.values()) {
            c.accept(monitorPointData);
        }
    }

    public void reset() {
        for (MonitorPointData monitorPoint : monitorPoints.values()) {
            monitorPoint.reset();
        }
    }

    public void match(long item, double diff) {
        for (MonitorPointData monitorPoint : monitorPoints.values()) {
            if (monitorPoint.getMonitorPoint().hasDataFrom(item)) {
                monitorPoint.getSketch().match(item, diff);
                break;
            }
        }
    }

    public void prepareForAccuracyEstimation() {
        apply((monitorPointData) -> monitorPointData.getSketch().getPrecisionEstimationData().prepare());
    }

    public void prepareForAccuracyEstimationForLevel(int level) {
        apply((monitorPointData) -> monitorPointData.getSketch().getPrecisionEstimationData().prepareForALevel(level));
    }

    public MonitorPointData getAMonitorPoint() {
        return monitorPoints.values().iterator().next();
    }

    public Collection<MonitorPointData> getMonitorPoints() {
        return monitorPoints.values();
    }
}
