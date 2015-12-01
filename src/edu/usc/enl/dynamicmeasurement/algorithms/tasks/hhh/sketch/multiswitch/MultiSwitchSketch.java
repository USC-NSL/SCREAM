package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.HHHAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 8/19/13
 * Time: 10:58 AM
 */
public class MultiSwitchSketch extends HHHAlgorithm {
    private Map<MonitorPoint, HierarchicalCountMinSketch> switchSketchMap;
    private HierarchicalCountMinSketch prototype;

    public MultiSwitchSketch(Element element) {
        super(element);
        int depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
        init(threshold, getTaskWildcardPattern(), Util.getNetwork().getMonitorPoints(), depth, WildcardPattern.TOTAL_LENGTH, 1);
    }

    @Override
    protected void update() {

    }

    public MultiSwitchSketch(double threshold, WildcardPattern taskWildcardPattern1, Collection<MonitorPoint> monitorPoints,
                             int depth, int U, int gran) {
        super(threshold, taskWildcardPattern1);
        init(threshold, taskWildcardPattern1, monitorPoints, depth, U, gran);
    }

    private void init(double threshold, WildcardPattern taskWildcardPattern1, Collection<MonitorPoint> monitorPoints, int depth, int U, int gran) {
        switchSketchMap = new HashMap<>();
        int minWidth = Integer.MAX_VALUE;
        for (MonitorPoint monitorPoint : monitorPoints) {
            minWidth = Math.min(minWidth, monitorPoint.getCapacity() / depth);
        }
        prototype = new HierarchicalCountMinSketch(threshold, taskWildcardPattern1, minWidth, depth, U, gran, null, null);
        for (MonitorPoint monitorPoint : monitorPoints) {
            switchSketchMap.put(monitorPoint, new HierarchicalCountMinSketch(threshold, getTaskWildcardPattern(), minWidth,
                    depth, U, gran, prototype.getHasha(), prototype.getHashb()));
        }
    }

    public int getSize() {
        return 0;
    }

    @Override
    public void match(long item, double diff) {
        for (Map.Entry<MonitorPoint, HierarchicalCountMinSketch> entry : switchSketchMap.entrySet()) {
            if (entry.getKey().hasDataFrom(item)) {
                entry.getValue().match(item, diff);
                break;
            }
        }
    }

    @Override
    public Collection<WildcardPattern> findHHH() {
        //sum counters in all level
        prototype.fillCounters(switchSketchMap.values());
        return prototype.findHHH();
    }

    @Override
    public void reset() {
        for (HierarchicalCountMinSketch hierarchicalCountMinSketch : switchSketchMap.values()) {
            hierarchicalCountMinSketch.reset();
        }
    }

    public double estimateAccuracy() {
        return prototype.estimateAccuracy();
    }
}
