package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 3/3/13
 * Time: 7:46 AM
 * <p>
 * Translated from C++ implementation in http://www2.research.att.com/~marioh/frequent-items/index.html
 */
public class HierarchicalCountMinSketch extends AbstractHierarchicalCountMinSketch {
    protected int[][] counts;

    public static final int COUNTER_SIZE = 1;

    public HierarchicalCountMinSketch(double threshold, WildcardPattern taskWildcardPattern1, int depth, int U, int gran, int[][] hashaa, int[][] hashbb, int capacity) {
        super(threshold, taskWildcardPattern1, depth, U, gran, hashaa, hashbb, capacity, COUNTER_SIZE);
        initCounters();
    }

    public HierarchicalCountMinSketch(double threshold, WildcardPattern taskWildcardPattern1, int width, int depth, int U, int gran, int[][] hashaa, int[][] hashbb) {
        super(threshold, taskWildcardPattern1, width, depth, U, gran, hashaa, hashbb);
        initCounters();
    }

    public HierarchicalCountMinSketch(Element element) {
        super(element);
        initCounters();
    }

    @Override
    protected int getACounterSize(Element element) {
        return COUNTER_SIZE;
    }

    @Override
    protected void update() {

    }


    /**
     * add counters from subsketches to this
     *
     * @param subSketches
     */
    public void fillCounters(Collection<? extends AbstractHierarchicalCountMinSketch> subSketches) {
        for (int[] count1 : this.counts) {
            Arrays.fill(count1, 0);
        }
        this.sum = 0;
        for (AbstractHierarchicalCountMinSketch subSketch : subSketches) {
            int[][] counts2 = ((HierarchicalCountMinSketch) subSketch).counts;
            for (int i = 0; i < counts2.length; i++) {
                int[] count = counts2[i];
                for (int j = 0; j < count.length; j++) {
                    this.counts[i][j] += count[j];
                }
            }
            this.sum += subSketch.sum;
        }
    }

    @Override
    protected void computeSum() {
        //it is computed on match
    }

    @Override
    public double getFalseProbability(double newSum, HHHOutput hhh, int level, int[] badCounters) {
//        int level = getLevel(hhhOutput.getWildcardPattern().getWildcardNum());
        double descendantSum = 0;
        for (Map.Entry<Integer, List<HHHOutput>> entry : hhh.getDescendantHHHs().getResult().entrySet()) {
            for (HHHOutput hhhOutput : entry.getValue()) {
                descendantSum += hhhOutput.getWildcardPattern().getWeight();
            }
        }
        double falseProbability = 1;
        for (int index : hhh.getAllCounters()) {
            double excess = 0;
            try {
                excess = counts[level][index] - descendantSum - threshold - (badCounters[index] - hhh.getWildcardPattern().getWeight() - descendantSum);
            } catch (Exception e) {
                System.out.println(level);
                System.out.println(counts[level][index]);
                System.out.println(badCounters);
                System.out.println(hhh.getWildcardPattern());
                System.out.println(precisionEstimationData.hhTree.size());
                System.out.println("--------------");
                for (Integer integer : precisionEstimationData.hhTree.keySet()) {
                    System.out.println(integer);
                }
                System.out.println("--------------");
                for (Integer integer : precisionEstimationData.badCounters.keySet()) {
                    System.out.println(integer);
                }
                e.printStackTrace();
            }
            if (excess > 0) {
                falseProbability *= Math.min(1, newSum / (capWidth * excess));
            }
        }
        return falseProbability;
    }

    @Override
    protected void createCounters(int levels) {
        counts = new int[levels][];
    }

    @Override
    protected void createCounterAt(int width, int depth, int i) {
        counts[i] = new int[depth * width];
    }


    @Override
    public void reset() {
        super.reset();
        //reset all sketches
        for (int[] count : counts) {
            if (count != null) {
                Arrays.fill(count, 0);
            }
        }
    }

    public void match(long item, double diff) {
        int i, j, offset;
        item = realToCounterInput(item);

        sum += diff;
        if (!zeroWidth) {
            for (i = 0; i < levels; i++) {
                offset = 0;
                if (isExactCounter(i)) {
                    if (item < counts[i].length) {
                        counts[i][(int) item] += diff; //reaching here item is small enough
                    } else {
                        System.err.println("Error: large exact item " + item + " in array size " + counts[i].length);
                        System.exit(1);
                    }
                    // keep exact counts at high levels in the hierarchy
                } else {

                    for (j = 0; j < depth; j++) {
//                        try {
                        counts[i][(PIRandomGenerator.hash31(hasha[i][j], hashb[i][j], item)
                                % capWidth) + offset] += diff;
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            System.out.println(item + "," + i + "," + taskWildcardPattern + "," + getStep() + "," + offset + "," +
//                                    (PIRandomGenerator.hash31(hasha[i][j], hashb[i][j], item)
//                                            % capWidth) + "," + capWidth);
//                        }
                        // this can be done more efficiently if the width is a power of two
                        offset += capWidth;
                    }
                }
                item >>>= gran;
            }
        }
    }


    public long CMH_count(int level, long item, IntegerWrapper index, int[] indexArray) {
        // return an estimate of item at level depth
        int j;
        int offset;
        long estimate;

        if (level >= levels) {
            return (sum);
        }
        if (zeroWidth) {
            return sum;
        }
        if (isExactCounter(level)) { // use an exact count if there is one
            index.setValue((int) item);
            Arrays.fill(indexArray, 0);
            indexArray[0] = index.getValue();
            return (counts[level][(int) item]);
        }
        // else, use the appropriate sketch to make an estimate
        offset = 0;//because all for a level for all hash functions are in 1D array
        int index2 = (PIRandomGenerator.hash31(hasha[level][0], hashb[level][0], item) % capWidth) + offset;
        index.setValue(index2);
        estimate = counts[level][index2];
        indexArray[0] = index2;
        for (j = 1; j < this.depth; j++) {
            offset += capWidth;
            index2 = (PIRandomGenerator.hash31(hasha[level][j], hashb[level][j], item) % capWidth) + offset;
            int v = counts[level][index2];
            indexArray[j] = index2;
            if (estimate > v) {
                estimate = v;
                index.setValue(index2);
            }
        }
        return estimate;
    }

    @Override
    public int getCount(int level, int index) {
        try {
            return counts[level][index];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

}
