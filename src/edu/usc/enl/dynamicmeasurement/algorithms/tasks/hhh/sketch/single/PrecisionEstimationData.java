package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.LongWrapper;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 2:12 PM
 */
public class PrecisionEstimationData {
    protected final AbstractHierarchicalCountMinSketch sketch;
    protected final TreeMap<Integer, List<LevelHH>> hhTree;
    protected Map<Integer, Double> levelHhsum;
    protected Map<Integer, Double> levelHhCollisionRatio;
    //    private HHFinder hhFinder;
    protected Map<Integer, int[]> badCounters = new HashMap<>();
    protected boolean[] skipBadCountersOnce;

    private Map<Integer, Integer> tempCountersUsage = new HashMap<>();

    public PrecisionEstimationData(AbstractHierarchicalCountMinSketch sketch) {
        levelHhsum = new HashMap<>();
        levelHhCollisionRatio = new HashMap<>();
        hhTree = new TreeMap<>();
        this.sketch = sketch;
//        this.hhFinder = hhFinder;
    }

    public Map<Integer, Double> getLevelHhsum() {
        return levelHhsum;
    }

    public void setLevelHhsum(Map<Integer, Double> levelHhsum) {
        this.levelHhsum = levelHhsum;
    }

    public Map<Integer, Double> getLevelHhCollisionRatio() {
        return levelHhCollisionRatio;
    }

    public void setLevelHhCollisionRatio(Map<Integer, Double> levelHhCollisionRatio) {
        this.levelHhCollisionRatio = levelHhCollisionRatio;
    }

    public void reset() {
        hhTree.clear();
        levelHhsum.clear();
        levelHhCollisionRatio.clear();
    }

    public TreeMap<Integer, List<LevelHH>> getHhTree() {
        return hhTree;
    }


    public void fixLevelHHSum() {
        if (!sketch.isZeroWidth()) {
//            hhFinder.invoke(this);
            if (hhTree.isEmpty()) { //note that hhtree has hhs in each level
                sketch.CMH_recursive(sketch.getLevels(), 0, new HHHSet(), new LongWrapper(0), hhTree, sketch.getThreshold());
            }

            int[] badCountersBitmap = new int[sketch.getWidth2() * sketch.getDepth()];
            for (Map.Entry<Integer, List<LevelHH>> entry : hhTree.entrySet()) {
                int level = entry.getKey();
                if (!sketch.isExactCounter(level)) {//finding hhSum for exact levels is meaningless
                    List<LevelHH> hhs = entry.getValue();
                    sketch.findBadCounters2(hhs, badCountersBitmap, level);
                    double hhSum = 0;
                    int count = 0;
                    for (LevelHH hh : hhs) {
                        double weight = hh.getWeight();
                        if (weight - (badCountersBitmap[hh.getCounterIndex()] - weight) >= sketch.getThreshold()) { //only one used my min counter
                            hhSum += Math.max(0, hh.getWeight());
                            count++;
                        }
                    }
                    if (hhSum > sketch.getSum() || sketch.getCapWidth() <= count) {
                        hhSum = 0;
//                    System.err.println("HHSum without collision is > sum");
                    } else {
                        hhSum = unBiasHHSum(hhSum, count);
                    }
                    if (hhSum < 0) {
                        hhSum = 0;
                    }
                    levelHhsum.put(level, hhSum);
                }
            }
        }
        for (int i = 0; i < sketch.getFreeLim(); i++) {
            Double v = levelHhsum.get(i);
            if (v == null || v > sketch.getSum() || v < 0) {
                levelHhsum.put(i, 0d);
            }
        }
    }

    protected double unBiasHHSum(double hhSum, int count) {
        hhSum = (sketch.getCapWidth() * hhSum - count * sketch.getSum()) / (sketch.getCapWidth() - count);
        return hhSum;
    }

    public void prepareForALevel(int level) {
        if (skipBadCountersOnce == null) {
            skipBadCountersOnce = new boolean[sketch.getWidth2() * sketch.getDepth()];
        }
        Arrays.fill(skipBadCountersOnce, false);
    }

    public void prepare() {
        fixLevelHHSum();
        findBadCounters2(hhTree);
    }

    public void updateAccuracy(Map<Integer, List<HHHOutput>> result, int outputSize) {
        if (outputSize == 0) {
            sketch.lastAccuracy = 1;
        } else {
//            findBadCounters(result);
            //now estimate accuracy based on hhTree and res
            double precision = 0;
            prepare();

            for (Map.Entry<Integer, List<HHHOutput>> entry : result.entrySet()) {
                prepareForALevel(entry.getKey());
                precision += getLevelAccuracy(entry.getValue(), entry.getKey());
            }
            sketch.lastAccuracy = precision / outputSize;
        }
    }

    protected double getLevelAccuracy(List<HHHOutput> levelHHHs, int level) {
        if (levelHHHs == null || levelHHHs.size() == 0) {
            return 0;
        }

        //now find how many bad hhs got in the level
//        double falseScale;
//        if (sketch.isExactCounter(level)) {
//            falseScale = 0;
//        } else {
//            double newSum = sketch.getSum() - getLevelHhsum().get(level);
//            if (newSum == 0) {
////                    System.out.println("0 new sum");
//                falseScale = 0;
//            } else {
//                falseScale = getFalseScale(newSum, levelHHHs, level);
//            }
//        }

        double falseScale = 1;

        //now for each hhh find its precision
        double precision = 0;
        for (HHHOutput hhh : levelHHHs) {
            precision += getHHHAccuracy(falseScale, hhh, level);
        }
//        System.out.println(level + ">" + precision + "," + levelHHHs.size());
        return precision;
    }

    public double getFalseScale(Double newSum, Collection<HHHOutput> levelHHHs, int level) {
        if (sketch.zeroWidth) {
            return 1;
        }
        double count = 0;
        for (HHHOutput levelHHH : levelHHHs) {
            double falseProbability = sketch.getFalseProbability(newSum, levelHHH, level, badCounters.get(level));
            if (falseProbability >= 0.9) {
                count++;
            }
        }
        return count / levelHHHs.size();
    }

    public double getHHHAccuracy(double falseScale, HHHOutput hhh, int level) {
        double weight = hhh.getWildcardPattern().getWeight();
        int[] badCountersBitmap = badCounters.get(level);
        double descendantSum = 0;
        for (Map.Entry<Integer, List<HHHOutput>> entry : hhh.getDescendantHHHs().getResult().entrySet()) {
            for (HHHOutput hhhOutput : entry.getValue()) {
                descendantSum += hhhOutput.getWildcardPattern().getWeight();
            }
        }
        if (badCountersBitmap == null) {
            return 0;
        }
        double precision = 0;
        //note badcountersbitmap also has my weight
        if (weight - (badCountersBitmap[hhh.getMinIndex()] - weight - descendantSum) < sketch.getThreshold()
                && !skipBadCountersOnce[hhh.getMinIndex()]) { //only one used my min counter
            //otherwise only one should be considered, but how?
            skipBadCountersOnce[hhh.getMinIndex()] = true;
            badCountersBitmap[hhh.getMinIndex()] -= weight;
            precision = getAFringeHHHAccuracy(falseScale, hhh, level);
            badCountersBitmap[hhh.getMinIndex()] += weight;
        } else {
            precision = getAFringeHHHAccuracy(falseScale, hhh, level);
        }
//        System.out.println(precision);
//        if (level > 0) {
//            System.out.println(sketch.getStep() + "," + hhh.getWildcardPattern().toStringNoWeight() + "," + hhh.getWildcardPattern().getWeight()+","+precision);
//        }
        if (hhh.getDescendantHHHs() != null && hhh.getDescendantHHHs().size() != 0 && !sketch.zeroWidth) {
            precision = getInternalHHHAccuracy(hhh, precision);
        }
//        System.out.println(getStep() + "," + hhh.getWildcardPattern() + "," + hhh.getDescendantHHHs().size() + "," + precision);
//        if (level > 0) {
//            System.out.println(sketch.getStep() + "," + hhh.getDescendantHHHs().size() + "," + hhh.getWildcardPattern().toStringNoWeight() + "," + hhh.getWildcardPattern().getWeight() + "," + precision);
//        }
        return precision;
    }

    private double getInternalHHHAccuracy(HHHOutput hhh, double accuracy) {
        double p = 0.1;
        double errorBoundDenominator = sketch.getCapWidth() * Math.pow(1 - p, 1.0 / sketch.depth);
        // else check the the descendant hhh weight and see if without overestimation
        int blockSize = 1 << sketch.gran;
        // a child may become an hhh
        //find estimate for children
        for (int i = 0; i < blockSize; i++) {
            accuracy = updateAccuracyBasedOnChild(hhh, accuracy, errorBoundDenominator, i);
        }
        return accuracy;
    }

    private double updateAccuracyBasedOnChild(HHHOutput hhh, double precision, double errorBoundDenominator, int i) {
        //note that this works for gran=1 as a parent can remain hhh even if one of >2 children is probably a hhh
        //for each child
        WildcardPattern wildcardPattern = hhh.getWildcardPattern();
        long childData = (wildcardPattern.getData() << sketch.gran) + i;
        int childWildcardNum = wildcardPattern.getWildcardNum() - sketch.gran;
        long item = sketch.realToCounterInput(childData << childWildcardNum) >>> childWildcardNum;
        int childLevel = sketch.getLevel(childWildcardNum);
        long estcount = sketch.CMH_count(childLevel, item);
        estcount = Math.min(estcount, sketch.getChildSum(Math.max(0, childLevel - 3), childLevel, item));
        if (estcount > sketch.getThreshold()) {
            //find the hhhs below it
            WildcardPattern childWildcardPattern = new WildcardPattern(childData, childWildcardNum, 0);
            //for each hhh find a lower bound for real data
            //sum the lower bound
            long descendantSumLowerBound = 0;
            for (Map.Entry<Integer, List<HHHOutput>> entry : hhh.getDescendantHHHs().getResult().entrySet()) {
                List<HHHOutput> hhhs = entry.getValue();
                for (HHHOutput descendantHHH : hhhs) {
                    if (childWildcardPattern.match(descendantHHH.getWildcardPattern())) {
                        int descendantLevel = sketch.getLevel(descendantHHH.getWildcardPattern().getWildcardNum());
                        if (sketch.isExactCounter(descendantLevel)) {
                            descendantSumLowerBound += descendantHHH.getWildcardPattern().getWeight();
                        } else {
                            double newSum = sketch.getSum() - getLevelHhsum().get(descendantLevel);
//                                if (newSum == 0) {
////                                    System.out.println("0 new sum");
//                                }
                            double errorBound = newSum / errorBoundDenominator;
                            descendantSumLowerBound += Math.max(sketch.getThreshold(), descendantHHH.getWildcardPattern().getWeight() - errorBound);
                        }
                    }
                }
            }
            //see if child-lower bound becomes an hh with high probabilty
            if (estcount - descendantSumLowerBound > sketch.getThreshold()) {
                precision /= 2;
            }
        }
        return precision;
    }

    public double getAFringeHHHAccuracy(double falseScale, HHHOutput hhh, int level) {
        double falseProbability;
        if (sketch.isExactCounter(level)) {
            falseProbability = 0;
        } else {
            if (sketch.zeroWidth) {
                return 0;
            }
            double newSum = 0;
            newSum = sketch.getSum() - getLevelHhsum().get(level);
            if (newSum == 0) {
//                    System.out.println("0 new sum");
                falseProbability = 0;
            } else {
                falseProbability = sketch.getFalseProbability(newSum, hhh, level, badCounters.get(level));
//                falseProbability *= falseScale;
            }
        }
        return (1 - falseProbability);
        // / 2) * Math.pow(1 - collisionsRatio, 2);
    }

//    public static void findBadCounters(Collection<HHHOutput> levelHHH, int[] badCountersBitmap, AbstractHierarchicalCountMinSketch sketch, int level) {
//        Arrays.fill(badCountersBitmap, 0);
//        for (HHHOutput hhh : levelHHH) {
//            for (int i : hhh.getAllCounters()) {
//                badCountersBitmap[i] += Math.min(sketch.getCount(level, i), hhh.getWildcardPattern().getWeight());
//            }
//        }
//
////            for (HHHOutput hhh : entry.getValue()) {
////                for (int i : hhh.getAllCounters()) {
////                    Integer usage = tempCountersUsage.get(i);
////                    if (usage == null) {
////                        usage = 0;
////                    }
////                    usage++;
////                    tempCountersUsage.put(i, usage);
////                }
////            }
////            for (Map.Entry<Integer, Integer> entry2 : tempCountersUsage.entrySet()) {
////                if (entry2.getValue() > 1) {
////                    badCountersBitmap[entry2.getKey()] = true;
////                }
////            }
//
//    }

    public void findBadCounters(Map<Integer, ? extends Collection<HHHOutput>> levelHHH) {
        for (Map.Entry<Integer, ? extends Collection<HHHOutput>> entry : levelHHH.entrySet()) {
            int[] badCountersBitmap = badCounters.get(entry.getKey());
            if (badCountersBitmap == null) {
                badCountersBitmap = new int[sketch.getWidth2() * sketch.getDepth()];
                badCounters.put(entry.getKey(), badCountersBitmap);
            }
            this.sketch.findBadCounters(entry.getValue(), badCountersBitmap, entry.getKey());
        }
    }

    public void findBadCounters2(Map<Integer, ? extends Collection<LevelHH>> levelHHH) {
        for (Map.Entry<Integer, ? extends Collection<LevelHH>> entry : levelHHH.entrySet()) {
            int[] badCountersBitmap = badCounters.get(entry.getKey());
            if (badCountersBitmap == null) {
                badCountersBitmap = new int[sketch.getWidth2() * sketch.getDepth()];
                badCounters.put(entry.getKey(), badCountersBitmap);
            }
            this.sketch.findBadCounters2(entry.getValue(), badCountersBitmap, entry.getKey());
        }
    }
}
