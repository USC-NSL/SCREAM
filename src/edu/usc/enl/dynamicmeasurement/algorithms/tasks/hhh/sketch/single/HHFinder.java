package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import edu.usc.enl.dynamicmeasurement.util.LongWrapper;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/8/13
 * Time: 5:31 PM
 */
public class HHFinder {
    private AbstractHierarchicalCountMinSketch sketch;

    public HHFinder(AbstractHierarchicalCountMinSketch sketch) {
        this.sketch = sketch;
    }

    public HHFinder invoke(PrecisionEstimationData data) {
        double currentThreshold = sketch.getThreshold();

        //if hhtree is empty fill it using initial threshold. If it was really empty the overhead is not much
        TreeMap<Integer, List<LevelHH>> hhTree = data.getHhTree();
        Map<Integer, Double> levelHHSum = data.getLevelHhsum();
        Map<Integer, Double> levelHHCollisionRatio = data.getLevelHhCollisionRatio();
        if (hhTree.isEmpty()) {
            sketch.CMH_recursive(sketch.getLevels(), 0, new HHHSet(), new LongWrapper(0), hhTree, currentThreshold);
//            for (LevelHH levelHH : hhTree.get(1)) {
//                System.out.println(levelHH.temp);
//            }
        }
        boolean firstGuess = true;
        Map<Integer, Double> oldLevelHHSum = null;
        Map<Integer, Double> oldLevelCollisionRatio = null;
//        while (true) {
            //fill levelhhSum and level hh collision
            double totalSum = getHHSumAndCollisions(hhTree, levelHHSum, levelHHCollisionRatio);
            double collisionsNumMin = 0;
            double hhSumMin = 0;
            long sketchSum = sketch.getSum();


            if (levelHHCollisionRatio.size() > 0) {
                collisionsNumMin = Double.MAX_VALUE;
                hhSumMin = Double.MAX_VALUE;
                for (Map.Entry<Integer, Double> entry : levelHHCollisionRatio.entrySet()) {
                    Double sum = levelHHSum.get(entry.getKey());
                    if (sum > 0 && hhSumMin > sum && sum < sketchSum * 0.99) {
                        hhSumMin = sum;
                        collisionsNumMin = entry.getValue();
                    }
                }
            }

            /*if (false && thresholdCondition(currentThreshold, sketchSum) && collisionsNumMin < 0.99 &&
                    (levelHHSum.size() == 0 || totalSum / levelHHSum.size() / sketchSum <= 0.99) &&
                    hhSumMin / sketchSum < 0.99) {
                //can still continue
                if (firstGuess) {
                    oldLevelHHSum = new HashMap<>();
                    oldLevelCollisionRatio = new HashMap<>();
                    oldLevelHHSum.putAll(levelHHSum);
                    oldLevelCollisionRatio.putAll(levelHHCollisionRatio);
                    firstGuess = false;
                } else {
                    //need to keep the current solution somewhere
                    for (Map.Entry<Integer, Double> entry : levelHHCollisionRatio.entrySet()) {
                        Integer key = entry.getKey();
                        Double collisionRatio = entry.getValue();
                        Double newHHSum = levelHHSum.get(key);
                        if (newHHSum / sketchSum < 0.99) {
                            Double v = oldLevelHHSum.get(key);
                            if (v != null) {
                                if (v < newHHSum) {
                                    oldLevelCollisionRatio.put(key, collisionRatio);
                                    oldLevelHHSum.put(key, newHHSum);
                                }
                            } else {
                                oldLevelCollisionRatio.put(key, collisionRatio);
                                oldLevelHHSum.put(key, newHHSum);
                            }
                        }
                    }
                    levelHHSum.clear();
                    levelHHCollisionRatio.clear();
                }
                hhTree.clear();
                currentThreshold /= 2;
                sketch.CMH_recursive(sketch.getLevels(), 0, new HHHSet(), new LongWrapper(0), hhTree, currentThreshold);
            } else {*/
                //return the old hhsum and collision rates
//                if (firstGuess) {
                    data.setLevelHhsum(levelHHSum);
                    data.setLevelHhCollisionRatio(levelHHCollisionRatio);
//                } else {
//                    data.setLevelHhsum(oldLevelHHSum);
//                    data.setLevelHhCollisionRatio(oldLevelCollisionRatio);
//                }
                return this;
//            }
//        }
    }

    protected boolean thresholdCondition(double currentThreshold, long sketchSum) {
        return currentThreshold > sketchSum / sketch.getCapWidth() / 2;
    }

    protected double getHHSumAndCollisions(TreeMap<Integer, List<LevelHH>> hhTree, Map<Integer, Double> currentLevelHhSum,
                                           Map<Integer, Double> currentLevelHhCollision) {
        double totalSum = 0;
        int[] badCountersBitmap = new int[sketch.getCapWidth() * sketch.getDepth()];
        for (Map.Entry<Integer, List<LevelHH>> hhTreeEntry : hhTree.entrySet()) {
            Integer level = hhTreeEntry.getKey();
            if (!sketch.isExactCounter(level)) {//finding hhSum for exact levels is meaningless
                List<LevelHH> hhs = hhTreeEntry.getValue();
                for (LevelHH hh : hhs) {
                    totalSum += hh.getWeight();
                }

                sketch.findBadCounters2(hhs, badCountersBitmap, level);

                double hhSum = 0;
                int count = 0;
                for (Iterator<LevelHH> iterator = hhs.iterator(); iterator.hasNext(); ) {
                    LevelHH hh = iterator.next();
                    double weight = hh.getWeight();
                    if (weight - (badCountersBitmap[hh.getCounterIndex()] - weight) >= sketch.getThreshold()) { //only one used my min counter
                        hhSum += Math.max(0, hh.getWeight());
                        count++;
                    } else {
//                        iterator.remove();
                    }
                }

                //solve: y=x-n(s-y)/w
                //note that if x/n>s/w, this becomes negative and we set it to zero
                if (hhSum > sketch.getSum() || sketch.getCapWidth() <= count) {
                    hhSum = 0;
//                    System.err.println("HHSum without collision is > sum");
                } else {
                    hhSum = unBiasHHSum(hhSum, count);
                }
                if (hhSum < 0) {
                    hhSum = 0;
                }
                int collisions = hhs.size() - count;

                currentLevelHhSum.put(level, hhSum);
                currentLevelHhCollision.put(level, 1.0 * collisions / hhs.size());
            }
        }
        return totalSum;
    }

    protected double unBiasHHSum(double hhSum, int count) {
        hhSum = (sketch.getCapWidth() * hhSum - count * sketch.getSum()) / (sketch.getCapWidth() - count);
        return hhSum;
    }


}
