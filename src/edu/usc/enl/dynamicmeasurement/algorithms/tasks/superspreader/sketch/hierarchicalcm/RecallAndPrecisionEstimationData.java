package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.LevelHH;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.PrecisionEstimationData;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/4/2014
 * Time: 4:53 PM
 */
public class RecallAndPrecisionEstimationData extends PrecisionEstimationData {
    private final boolean estimateRecall;

    public RecallAndPrecisionEstimationData(AbstractHierarchicalCountMinSketch sketch,
                                            boolean estimateRecall) {
        super(sketch);
        this.estimateRecall = estimateRecall;
    }

    @Override
    public void updateAccuracy(Map<Integer, List<HHHOutput>> result, int outputSize) {
        //update recall before precision before precision mess with hhhtree
        if (estimateRecall) {
            updateRecall();
        }

        super.updateAccuracy(result, outputSize);
    }

    private void updateRecall() {
        //go through tree once more!
        HashMap<Integer, Double> hhTree1 = new HashMap<>();
        for (Map.Entry<Integer, List<LevelHH>> entry : hhTree.entrySet()) {
            hhTree1.put(entry.getKey(), entry.getValue().stream().mapToDouble(LevelHH::getWeight).sum());
        }
        ((SSHierarchicalCountMin) sketch).CMH_recursive2(sketch.getLevels(), 0, new IntegerWrapper(0), hhTree1, sketch.getThreshold(), 0, new double[7]);
    }

    public double getAFringeHHHAccuracy(double falseScale, HHHOutput hhh, int level) {
        double falseProbability;
        SSHierarchicalCountMin sketch1 = (SSHierarchicalCountMin) sketch;
        double threshold = sketch1.getThreshold();
        if (sketch.isExactCounter(level)) {
            //just the cc distribution
            falseProbability = 1 - sketch1.normalDistribution.cumulativeProbability((hhh.getWildcardPattern().getWeight() - threshold) / threshold / sketch1.getCCSigma());
//            sketch1.CMH_count(level, hhh.getWildcardPattern().getData(), null, sketch1.indexArray);
//            for (int i = 0; i < sketch1.getDepth() / 2; i++) {
//                int excess = sketch1.countsCache[level][sketch1.indexArray[i]];
//                falseProbability *= 1 - sketch1.normalDistribution.cumulativeProbability((excess - threshold) / threshold);
//            }
        } else {
            if (sketch.isZeroWidth()) {
                return 0;
            }
            double newSum = 0;
            newSum = sketch.getSum() - getLevelHhsum().get(level);
            if (newSum <= 0) {
//                System.out.println("0 new sum");
                falseProbability = 0;
            } else {
                falseProbability = sketch1.getFalseProbability(newSum, hhh, level, badCounters.get(level));
//                falseProbability *= falseScale;
            }
        }
        return (1 - falseProbability);
        // / 2) * Math.pow(1 - collisionsRatio, 2);
    }


}
