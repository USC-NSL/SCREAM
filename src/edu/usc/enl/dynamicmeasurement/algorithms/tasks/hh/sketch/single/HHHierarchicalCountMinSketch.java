package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.sketch.single;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.HHAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import org.w3c.dom.Element;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 8/18/13
 * Time: 7:41 AM <br/>
 * For finding heavy hitters on a single switch by adapting the algorithm of finding hierarchical HHs
 */
public class HHHierarchicalCountMinSketch extends HHAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    private HierarchicalCountMinSketch2 countMinSketch;

    public HHHierarchicalCountMinSketch(Element element) {
        super(element);
        countMinSketch = new HierarchicalCountMinSketch2(element);
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

    public void setCapacity(int c) {
        countMinSketch.setCapacityShare(c);
    }

    @Override
    public double estimateAccuracy() {
        return countMinSketch.estimateAccuracy();
    }

    @Override
    public int getUsedResourceShare() {
        return countMinSketch.getUsedResourceShare();
    }

    @Override
    public void setCapacityShare(int c) {
        countMinSketch.setCapacityShare(c);
    }

    @Override
    public void finish() {
        countMinSketch.finish();
        super.finish();
    }

    public static class HierarchicalCountMinSketch2 extends HierarchicalCountMinSketch {

        public HierarchicalCountMinSketch2(Element element) {
            super(element);
        }

        public HierarchicalCountMinSketch2(double threshold, WildcardPattern taskWildcardPattern1, int depth, int U, int gran, int[][] hashaa, int[][] hashbb, int capacity) {
            super(threshold, taskWildcardPattern1, depth, U, gran, hashaa, hashbb, capacity);
        }

        @Override
        protected int computeU(int l) {
            return WildcardPattern.TOTAL_LENGTH - wildcardNum;
        }

        public boolean filterLevel(int level) {
            return level == 0;
        }

    }


}
