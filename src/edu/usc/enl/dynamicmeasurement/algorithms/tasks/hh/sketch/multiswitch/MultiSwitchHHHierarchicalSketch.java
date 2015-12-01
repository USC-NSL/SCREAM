package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.sketch.multiswitch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.HHAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.sketch.single.HHHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchSketch2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.SketchMultiSwitch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/8/13
 * Time: 5:10 PM <br/>
 * Implementing the heavy hitter detection algorithm using hierarchical sketches
 */
public class MultiSwitchHHHierarchicalSketch extends HHAlgorithm implements MultiSwitchTask.MultiSwitchTaskImplementation {
    protected MultiSwitchSketch3 countMinSketch;

    public MultiSwitchHHHierarchicalSketch(Element element) {
        super(element);
        countMinSketch = new MultiSwitchSketch3(element);
    }

    @Override
    public void match(long item, double diff) {
        countMinSketch.match(item, diff);
    }

    @Override
    public Collection<WildcardPattern> findHH() {
        return countMinSketch.findHHH();
    }

    @Override
    public void setStep(int step) {
        super.setStep(step);
        countMinSketch.setStep(step);
    }

    @Override
    public void setFolder(String folder) {
        super.setFolder(folder);
        countMinSketch.setFolder(folder);
    }

    @Override
    public void reset() {
        countMinSketch.reset();
    }

    @Override
    public void update(int step) {
        countMinSketch.doUpdate();
    }

    @Override
    public void setSum(double sum) {
        countMinSketch.setSum(sum);
    }

    @Override
    public void finish() {
        countMinSketch.finish();
        super.finish();
    }

    @Override
    public void setCapacityShare(Map<MonitorPoint, Integer> resource) {
        countMinSketch.setCapacityShare(resource);
    }

    @Override
    public void estimateAccuracy(Map<MonitorPoint, Double> accuracy) {
        countMinSketch.estimateAccuracy(accuracy);
    }

    @Override
    public double getGlobalAccuracy() {
        return countMinSketch.getGlobalAccuracy();
    }

    @Override
    public void getUsedResources(Map<MonitorPoint, Integer> resource) {
        countMinSketch.getUsedResources(resource);
    }

    /**
     * Just adapt the HHH solution by filtering the level of outputs
     */
    protected static class MultiSwitchSketch3 extends MultiSwitchSketch2 {

        protected MultiSwitchSketch3(Element element) {
            super(element);
        }

        @Override
        protected int computeU(int i) {
            return WildcardPattern.TOTAL_LENGTH - wildcardNum;
        }

        @Override
        protected boolean filterLevel(int level) {
            return level == 0;
        }

        protected void init(Collection<MonitorPoint> monitorPoints) {
            gran = 1;
            depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
            U = computeU(0);
            this.levels = (int) Math.ceil(1.0 * (getTaskWildcardPattern().getWildcardNum() - (WildcardPattern.TOTAL_LENGTH - U)) / gran);

            Map<MonitorPoint, AbstractHierarchicalCountMinSketch> map = new HashMap<>();
            monitorPoints.stream().filter(monitorPoint -> monitorPoint.hasDataFrom(this.getTaskWildcardPattern())).forEach(monitorPoint -> {
                map.put(monitorPoint, new HHHierarchicalCountMinSketch.HierarchicalCountMinSketch2(threshold, getTaskWildcardPattern(),
                        depth, U, gran, null, null, monitorPoint.getCapacity()));
            });
            this.sketchMultiSwitch = new SketchMultiSwitch(map);
        }

    }

}
