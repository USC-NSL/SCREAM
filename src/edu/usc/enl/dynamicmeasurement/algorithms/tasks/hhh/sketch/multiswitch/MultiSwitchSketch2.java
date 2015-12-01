package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.HHHAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHSet;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.MonitorPointData;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.SketchMultiSwitch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.util.ControlledBufferWriter;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.LongWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 8/19/13
 * Time: 10:58 AM
 */
public class MultiSwitchSketch2 extends HHHAlgorithm implements MultiSwitchTask.MultiSwitchTaskImplementation {
    public static final double DESCENDANT_ERROR_BOUND_P = 0.1;
    private final boolean useChildSum = false;
    protected SketchMultiSwitch sketchMultiSwitch;

    protected ControlledBufferWriter accuracyWriter;
    protected double lastAccuracy;
    protected long sum = 0;
    protected int levels;
    protected boolean cannotTraverseAll;
    protected int gran;
    protected int depth;
    protected DynamicWildcardPattern tempWildcardPattern = new DynamicWildcardPattern(0, 0, 0);
    protected int U;
    protected List<MonitorPointData> tempContributingMonitors = new ArrayList<>();
    private final int[] tempArray;

    public MultiSwitchSketch2(Element element) {
        super(element);
        Collection<MonitorPoint> monitorPoints = Util.getNetwork().getMonitorPoints();
        init(monitorPoints);
        tempArray = new int[depth];
    }

    @Override
    protected void update() {

    }

    @Override
    public void setFolder(String folder) {
        super.setFolder(folder);
        try {
            accuracyWriter = Util.getNewWriter(folder + "/acc.csv");
            //new PrintWriter(folder + "/acc.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Collection<WildcardPattern> findHHH() {
        prepareToFindHHH();

        HHHSet result = new HHHSet();
        CMH_recursive(levels, 0, result, new LongWrapper(0), threshold);

        sketchMultiSwitch.setMonitorPointsItems(result.getResult());

        List<WildcardPattern> output = AbstractHierarchicalCountMinSketch.fillOutput(result.getResult());
        updateAccuracy(result.getResult(), output.size());
        accuracyWriter.println(getStep() + "," + lastAccuracy);
        return output;
    }

    protected void prepareToFindHHH() {
        cannotTraverseAll = false;
        sketchMultiSwitch.prepare();
        sum = sketchMultiSwitch.getSum();
    }

    protected boolean filterLevel(int level) {
        return true;
    }

    protected void init(Collection<MonitorPoint> monitorPoints) {
        gran = 1;
        depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
        U = computeU(0);
        this.levels = (int) Math.ceil(1.0 * (getTaskWildcardPattern().getWildcardNum() - (WildcardPattern.TOTAL_LENGTH - U)) / gran);

        Map<MonitorPoint, AbstractHierarchicalCountMinSketch> map = new HashMap<>();
        monitorPoints.stream().filter(monitorPoint -> monitorPoint.hasDataFrom(this.getTaskWildcardPattern())).forEach(monitorPoint -> {
            map.put(monitorPoint, new HierarchicalCountMinSketch(threshold, getTaskWildcardPattern(),
                    depth, U, gran, null, null, monitorPoint.getCapacity()));
        });
        this.sketchMultiSwitch = new SketchMultiSwitch(map);
    }

    protected int computeU(int i) {
        return WildcardPattern.TOTAL_LENGTH;
    }


    @Override
    public void reset() {
        sum = 0;
        sketchMultiSwitch.reset();
    }

    @Override
    public void match(long item, double diff) {
        sketchMultiSwitch.match(item, diff);
    }

    public long CMH_count(int level, long item, int[] counts) {
        if (level >= levels) {
            return (sum);
        }
        tempContributingMonitors.clear();
        // return an estimate of item at level depth
        // find monitor points that match item
        int shifts = getRealWildcardNums(level);
        tempWildcardPattern.setData(getRealData(item, shifts));
        tempWildcardPattern.setWildcardNum(shifts);
        long sum = 0;
        Arrays.fill(counts, 0);
        for (MonitorPointData monitorPointData : sketchMultiSwitch.getMonitorPoints()) {
            if (monitorPointData.getMonitorPoint().hasDataFrom(tempWildcardPattern)) {
                sum += monitorPointData.getSketch().CMH_count(level, item);
                tempContributingMonitors.add(monitorPointData);
                for (int i = 0; i < depth; i++) {
                    counts[i] += monitorPointData.getSketch().getCount(level, monitorPointData.getSketch().indexArray[i]);
                }
            }
        }
        return sum;
    }

    protected long CMH_recursive(int level, long start, HHHSet res, LongWrapper childSum, double threshold) {
        // for finding heavy hitters, recursively descend looking
        // for ranges that exceed the threshold

        int i;
        int blockSize;
        long estcount;
        long itemShift;
        boolean gatherHH = filterLevel(level);
        double maxHHH = Math.min(1000, 50 * Math.ceil(sum / threshold));
//        if (level == 1) {
//            int shifts = getRealWildcardNums(level);
//            WildcardPattern wildcardPattern = new WildcardPattern(getRealData(start, shifts),
//                    shifts, 0);
//            if (wildcardPattern.toStringNoWeight().equals("0011000000000001100010000001100_")) {
//                System.out.println();
//            }
//        }
        int[] tempArray = new int[depth];
        estcount = CMH_count(level, start, tempArray);
        if (estcount >= threshold) {
            HHHSet childrenHHHs = new HHHSet();
            int minCounter = 0;
            if (tempContributingMonitors.size() == 1) {
                AbstractHierarchicalCountMinSketch sketch = tempContributingMonitors.get(0).getSketch();
                System.arraycopy(sketch.indexArray, 0, tempArray, 0, depth);
                minCounter = sketch.getIndexWrapper().getValue();
            }
            long descendantHHHSum = 0;
            if (level > 0) {
                blockSize = 1 << gran;
                itemShift = start << gran;
                // assumes that gran is an exact multiple of the bit dept
                long sum = 0;
                for (i = 0; i < blockSize; i++) {
                    if (res.size() + childrenHHHs.size() > maxHHH) {
                        cannotTraverseAll = true;
                        break;
                    }
                    descendantHHHSum += CMH_recursive(level - 1, itemShift + i, childrenHHHs, childSum, threshold);
                    sum += childSum.getValue();
                }
                if (useChildSum) {
                    estcount = Math.min(estcount, sum);
                }
            }
            res.merge(childrenHHHs);
            childSum.setValue(estcount);

            if (estcount - descendantHHHSum >= threshold && gatherHH) {
                if (res.size() < maxHHH) { // WHY? Haha now I understand because it can use all memory
                    int shifts = getRealWildcardNums(level);
                    WildcardPattern wildcardPattern = new WildcardPattern(getRealData(start, shifts),
                            shifts, estcount - descendantHHHSum);
                    MultiSwitchHHHOutput hhh = new MultiSwitchHHHOutput(wildcardPattern);

                    hhh.setAllCounters(tempArray);
                    hhh.setMinIndex(minCounter);
                    res.add(hhh, level);
                    //if it is exact counter and there is no hhh children it is for sure an HHH
//                    if (isExactCounter(level) && childrenHHHs.size() == 0) {
//                        hhh.setPrecision(1);
//                    }
                    hhh.setDescendantHHHs(childrenHHHs);
                }
                return estcount;
            }
            return descendantHHHSum;
        } else if (useChildSum) {
            if (estcount == 0) {
                childSum.setValue(0);
            } else {
                childSum.setValue(Math.min(estcount, getChildSum(Math.max(0, level - 3), level, start)));
            }
        }
        return 0;
    }

    protected int getRealWildcardNums(int level) {
        return gran * level + WildcardPattern.TOTAL_LENGTH - U;
    }

    protected long getRealData(long start, int shifts) {
        if (getTaskWildcardPattern().getWildcardNum() - shifts >= 0) {
            return start + (getTaskWildcardPattern().getData() <<
                    (getTaskWildcardPattern().getWildcardNum() - shifts));
        } else {
            return start;
        }
    }

    private long realToCounterInput(long item) {
        return sketchMultiSwitch.getAMonitorPoint().getSketch().realToCounterInput(item);
    }

    protected int getLevel(int wildcardNum) {
        return sketchMultiSwitch.getAMonitorPoint().getSketch().getLevel(wildcardNum);
    }

    protected long getChildSum(int minDepth, int depth, long start) {
        // if depth<maxDepth, call this method for each child
        // for each child get children sum and if their sum<child weight use their sum
        if (depth > minDepth) {
            long sum = 0;
            int blockSize = 1 << gran;
            long itemShift = start << gran;
            // assumes that gran is an exact multiple of the bit dept
            for (int i = 0; i < blockSize; i++) {
                sum += getChildSum(minDepth, depth - 1, itemShift + i);
            }
            long prediction = CMH_count(depth, start, tempArray);
//            if (sum < prediction) {
//                System.out.println("sum is smaller than estimation");
//            }
            return Math.min(prediction, sum);
        } else {
            return CMH_count(depth, start, tempArray);
        }
    }

    @Override
    public void finish() {
        super.finish();
        accuracyWriter.close();
    }

    protected void updateAccuracy(Map<Integer, List<HHHOutput>> result, int outputSize) {
//        if (getStep() == 9) {
//            System.out.println();
//        }
        if (outputSize > 0) {
            double precision = 0;
            sketchMultiSwitch.prepareForAccuracyEstimation();
            //now estimate accuracy based on hhTree and res
            List<Integer> sortedLevels = new ArrayList<>(result.keySet());
            Collections.sort(sortedLevels);
            for (Integer sortedLevel : sortedLevels) {
                sketchMultiSwitch.prepareForAccuracyEstimationForLevel(sortedLevel);
                precision += getLevelAccuracy(result.get(sortedLevel));
            }
            lastAccuracy = precision / outputSize;
        } else {
            lastAccuracy = 1;
        }
//        System.out.println(getStep() + "," + lastAccuracy);
    }

    private double getLevelAccuracy(List<HHHOutput> levelHHHs) {

        if (levelHHHs == null || levelHHHs.size() == 0) {
            return 0; //don't add any precision to monitor points
        }
        //now for each hhh find its precision
        double precision = 0;
        IntegerWrapper chernoffFalseScale = new IntegerWrapper(0);
        for (HHHOutput hhh : levelHHHs) {
            precision += getHHHAccuracy((MultiSwitchHHHOutput) hhh, chernoffFalseScale, levelHHHs.size());
        }
        return precision;
    }

    protected double getHHHAccuracy(MultiSwitchHHHOutput hhh, IntegerWrapper chernoffFalseScale, int count) {
        //first find accuracy of this hhh using bound on summation of error random variables
        //chernoff/hoeffding's inequality
        //For chernoff, need to scale all variables with the same scale factor
        //so lets use hoeffding P(collision>t)<exp(-2*n^2*t^2/sum(sum^2)
        //I don't like that the error bound is independent of mu (related to w)
        // but hopefully with larger w, mu become smaller and t is larger with fixed a_is
        // t=sum(a_i)-threshold-mu
        //hoeffding's inequality is very loose
        // ok so i go with chernoff (see https://www.cs.princeton.edu/courses/archive/fall09/cos521/Handouts/probabilityandcomputing.pdf)
        double mu = 0;
        double maxS = -1;
        int level = getLevel(hhh.getWildcardPattern().getWildcardNum());
        Map<MonitorPointData, Double> contributingMonitors = new HashMap<>();
        for (MonitorPointData monitorPoint : sketchMultiSwitch.getMonitorPoints()) {
            Map<Integer, Set<HHHOutput>> levelHHH = monitorPoint.getLevelHHH();
            if (levelHHH.containsKey(level) && levelHHH.get(level).contains(hhh)) { //this monitor point may not have any hhh in that level
                //scaling mu by hhh_sum will be 1/width
                double mu_i;
                AbstractHierarchicalCountMinSketch sketch = monitorPoint.getSketch();
                if (sketch.isExactCounter(level)) {
                    mu_i = 0;
                } else {
                    double tailSum = sketch.getSum();
                    if (sketch.isZeroWidth()) {
                        mu_i = tailSum;
                    } else {
                        tailSum -= monitorPoint.getLevelHhsum().get(level);
                        mu_i = tailSum / sketch.getCapWidth();
                    }
                    maxS = Math.max(tailSum, maxS);
                }
                mu += mu_i; //mu must be sum not average
                contributingMonitors.put(monitorPoint, mu_i);
            }
        }
        hhh.setContributingMonitors(contributingMonitors);

        double precision = 0;
        if (contributingMonitors.size() == 1) {
//            hhh.setOneMonitorPoint(oneMonitorPoint);
            MonitorPointData oneMonitorPoint = contributingMonitors.keySet().iterator().next();
//            Double falseScale = oneMonitorPoint.getFalseScales().get(level);
            precision = oneMonitorPoint.getSketch().getPrecisionEstimationData().getHHHAccuracy(1, hhh, level);
            oneMonitorPoint.addPrecision(precision);
        } else {
            precision = multiswitchPrecision(hhh, chernoffFalseScale, count, mu, maxS, level);
        }


//        System.out.println(getStep() + "," + hhh.getWildcardPattern().toStringNoWeight() + "," + hhh.getWildcardPattern().getWeight() + "," + precision);
        return precision;
    }

    protected double multiswitchPrecision(MultiSwitchHHHOutput hhh, IntegerWrapper chernoffFalseScale, int count, double mu, double maxS, int level) {
        double precision;
        hhh.setMaxS(maxS);
        hhh.setMu(mu);
        double falseProbability;
        if (maxS <= 0) {
            //there is no error
            precision = 1;
        } else {
            //w=(1+delta)*mu
            //note mu is average of error
            double delta = (hhh.getWildcardPattern().getWeight() - threshold) / mu - 1;
            if (delta <= 0) {
                falseProbability = 1;
                chernoffFalseScale.add(1);
                //I want falsescale to be f/n which is sum(1+2(i-1))/n for i=1 to f (as a streaming algorithm)
                precision = Math.max(0, (1 - falseProbability * (1 + 2 * (chernoffFalseScale.getValue())) / count));
            } else {
                falseProbability = chernoff(mu, maxS, delta, depth);
                precision = (1 - falseProbability);
            }
        }

        if (hhh.getDescendantHHHs().size() > 0) {
            precision = updateInternalHHHAccuracy(hhh, level, precision);

        } else {
            distributePrecisionForFringe(level, precision, hhh);
        }
        return precision;
    }

    public static double chernoff(double mu, double maxS, double delta, int d) {
        double falseProbability;
        falseProbability = Math.exp(-delta * delta / (2 + delta) * mu / maxS);
        falseProbability = Math.pow(falseProbability, d);
        return falseProbability;
    }

    @Override
    public void setStep(int step) {
        super.setStep(step);
        sketchMultiSwitch.setStep(step);
    }

    protected double updateInternalHHHAccuracy(MultiSwitchHHHOutput hhh, int level, double precision) {
        //need to check if the children will become hhh considering the error of descendant hhhs
        // else check the the descendant hhh weight and see if without overestimation
        int blockSize = 1 << gran;
        // a child may become an hhh
        //find estimate for children
        List<Map<MonitorPointData, Double>> errors = new ArrayList<>(blockSize);
        for (int i = 0; i < blockSize; i++) {
            Map<MonitorPointData, Double> contributingError = new HashMap<MonitorPointData, Double>();
            for (MonitorPointData monitorPointData : hhh.getContributingMonitors().keySet()) {
                contributingError.put(monitorPointData, 0d);
            }
            if (updateAccuracyBasedOnChild(hhh, i, contributingError)) {
                errors.add(contributingError);
            }
        }
        distributePrecisionForInternal(level, precision, hhh, errors);
        precision /= (1 << errors.size());
        return precision;
    }

    private void distributePrecisionForInternal(int level, double precision, MultiSwitchHHHOutput hhh, List<Map<MonitorPointData, Double>> errors) {
        Collection<MonitorPointData> contributingMonitors = hhh.getContributingMonitors().keySet();
        double errorSum = 0;
        for (MonitorPointData monitorPointData : contributingMonitors) {
            errorSum += monitorPointData.getErrorBound(level, hhh);
        }
        Map<MonitorPointData, Double> monitorPointDataPrecisionMap = new HashMap<>();
        if (errorSum == 0) {
            if (precision < 1) {
                System.err.println(this.getClass() + " precision should not be <1 if errorsum=0");
                //otherwise it does not make sense especially when updating based on errors later
                //this is a special case for exact counters
            }
            for (MonitorPointData monitorPointData : contributingMonitors) {
//                monitorPointData.addPrecision(precision);
                monitorPointDataPrecisionMap.put(monitorPointData, precision);
            }
        } else {
            int n = contributingMonitors.size();
            for (MonitorPointData monitorPointData : contributingMonitors) {
                double errorBound = monitorPointData.getErrorBound(level, hhh);
                double x = errorBound / errorSum;
                if (x >= 1.0 / n) {
//                    monitorPointData.addPrecision(precision);
                    monitorPointDataPrecisionMap.put(monitorPointData, precision);
                } else {
                    double p = x * (precision - 1) + 1;//linear from 1 to precision from 0 to 1/n
//                    monitorPointData.addPrecision(p);
                    monitorPointDataPrecisionMap.put(monitorPointData, p);
                }
            }
        }
        for (Map<MonitorPointData, Double> error : errors) {
            double sum = 0;
            for (Map.Entry<MonitorPointData, Double> entry : error.entrySet()) {
                sum += entry.getValue();
            }
            for (Map.Entry<MonitorPointData, Double> entry : error.entrySet()) {
                double p = entry.getValue() / sum * (-0.5) + 1;//linear from 1 to 1/2 from 0 to 1/n
                monitorPointDataPrecisionMap.put(entry.getKey(), monitorPointDataPrecisionMap.get(entry.getKey()) * p);
            }
        }
        for (Map.Entry<MonitorPointData, Double> entry : monitorPointDataPrecisionMap.entrySet()) {
            entry.getKey().addPrecision(entry.getValue());
        }
    }

    protected void distributePrecisionForFringe(int level, double precision, MultiSwitchHHHOutput hhh) {
        Collection<MonitorPointData> contributingMonitors = hhh.getContributingMonitors().keySet();
        double errorSum = 0;
        for (MonitorPointData monitorPointData : contributingMonitors) {
            errorSum += monitorPointData.getErrorBound(level, hhh);
        }
        if (errorSum == 0) {
            for (MonitorPointData monitorPointData : contributingMonitors) {
                monitorPointData.addPrecision(precision);
            }
        } else {
            int n = contributingMonitors.size();
            for (MonitorPointData monitorPointData : contributingMonitors) {
                double errorBound = monitorPointData.getErrorBound(level, hhh);
                double x = errorBound / errorSum;
                if (x >= 1.0 / n) {
                    monitorPointData.addPrecision(precision);
                } else {
                    monitorPointData.addPrecision(x * (precision - 1) + 1);//linear from 1 to precision from 0 to 1/n
                }
            }
        }
    }

    private boolean updateAccuracyBasedOnChild(MultiSwitchHHHOutput hhh, int i, Map<MonitorPointData, Double> contributingError) {
        //find estimate of child
        WildcardPattern wildcardPattern = hhh.getWildcardPattern();
        long childData = (wildcardPattern.getData() << gran) + i;
        int childWildcardNum = wildcardPattern.getWildcardNum() - gran;
        long item = realToCounterInput(childData << childWildcardNum) >>> childWildcardNum;
        int childLevel = getLevel(childWildcardNum);
        long estcount = CMH_count(childLevel, item, tempArray);
//        estcount = Math.min(estcount, getChildSum(Math.max(0, childLevel - 3), childLevel, item));

        //if it is >threshold, find if it could become hhh if considering error bound of descendant hhhs
        if (estcount > threshold) {
            //find all descendant of hhh that matches child i
            long descendantSumLowerBound = 0;
            WildcardPattern childWildcardPattern = new WildcardPattern(childData, childWildcardNum, 0);
            for (Map.Entry<Integer, List<HHHOutput>> entry : hhh.getDescendantHHHs().getResult().entrySet()) {
                List<HHHOutput> hhhs = entry.getValue();
                for (HHHOutput descendantHHH2 : hhhs) {
                    MultiSwitchHHHOutput descendantHHH = (MultiSwitchHHHOutput) descendantHHH2;
                    if (childWildcardPattern.match(descendantHHH.getWildcardPattern())) {
                        double weight = descendantHHH.getWildcardPattern().getWeight();
                        if (descendantHHH.isFromOneMonitorPoint()) {
                            //if the descendant hhh matches only one switch find its p error bound based on markov
                            MonitorPointData monitorPointData = descendantHHH.getContributingMonitors().keySet().iterator().next();
                            int descendantLevel = getLevel(descendantHHH.getWildcardPattern().getWildcardNum());
                            if (monitorPointData.getSketch().isExactCounter(descendantLevel)) {
                                descendantSumLowerBound += weight;
                            } else {
                                double errorBound = computeDescendantOneMonitorErrorBound(DESCENDANT_ERROR_BOUND_P, monitorPointData, descendantLevel);
                                contributingError.put(monitorPointData, contributingError.get(monitorPointData) + errorBound);
                                descendantSumLowerBound += Math.max(threshold, weight - errorBound);
                            }
                        } else {
                            if (descendantHHH.getMaxS() <= 0) { //this was exact on all counters
                                descendantSumLowerBound += weight;
                            } else {
                                //else find its p error bound based on chernoff
                                double errorBound = computeDescendantMultiMonitorErrorBound(DESCENDANT_ERROR_BOUND_P, descendantHHH);
                                descendantSumLowerBound += Math.max(threshold, weight - errorBound);
                                for (Map.Entry<MonitorPointData, Double> entry2 : descendantHHH.getContributingMonitors().entrySet()) {
                                    contributingError.put(entry2.getKey(), contributingError.get(entry2.getKey()) + errorBound / descendantHHH.getMu() * entry2.getValue());
                                }
                            }
                        }
                    }
                }
            }


            //now if estcount-descendantweight+descendant error>threshold. This child could be hhh so halve the precision
            if (estcount - descendantSumLowerBound > threshold) {
                return true;
            }
        }

        return false;
    }

    protected double computeDescendantMultiMonitorErrorBound(double p, MultiSwitchHHHOutput descendantHHH) {
        double delta = Math.sqrt(-3 * descendantHHH.getMaxS() / descendantHHH.getMu() * Math.log(p));
        return (1 + delta) * descendantHHH.getMu();
    }

    protected double computeDescendantOneMonitorErrorBound(double p, MonitorPointData monitorPointData, int descendantLevel) {
        double newSum = 0;
        newSum = sum - monitorPointData.getLevelHhsum().get(descendantLevel);
        AbstractHierarchicalCountMinSketch sketch = monitorPointData.getSketch();
        double errorBound;
        if (sketch.isZeroWidth()) {
            errorBound = newSum;
        } else {
            double errorBoundDenominator = sketch.getCapWidth() * Math.pow(1 - p, 1.0 / sketch.getDepth());
            errorBound = newSum / errorBoundDenominator;
        }
        return errorBound;
    }


    @Override
    public void setCapacityShare(Map<MonitorPoint, Integer> resource) {
        sketchMultiSwitch.setCapacityShare(resource);
    }

    @Override
    public void estimateAccuracy(Map<MonitorPoint, Double> accuracy) {
        Collection<MonitorPointData> monitorPoints = sketchMultiSwitch.getMonitorPoints();
        if (cannotTraverseAll) {//assume only the root has been detected and is a wrong hhh
            int n = monitorPoints.size();
            double[] errorBounds = new double[n];
            int i = 0;
            double errorSum = 0;
            for (MonitorPointData monitorPointData : monitorPoints) {
                double errorBound = 0;
                AbstractHierarchicalCountMinSketch sketch = monitorPointData.getSketch();
                if (sketch.isZeroWidth()) {
                    errorBound = sketch.getSum();
                } else {
                    errorBound = sketch.getSum() / sketch.getCapWidth();
                }
                errorBounds[i++] = errorBound;
                errorSum += errorBound;
            }
            i = 0;
            for (MonitorPointData monitorPointData : monitorPoints) {
                double x = errorBounds[i++] / errorSum;
                if (x >= 1.0 / n) {
                    accuracy.put(monitorPointData.getMonitorPoint(), 0d);
                } else {
                    accuracy.put(monitorPointData.getMonitorPoint(), x * (0 - 1) + 1); ////linear from 1 to precision from 0 to 1/n
                }
            }
        } else {
            for (MonitorPointData m : monitorPoints) {
                double averagePrecision = m.getAveragePrecision();
//                if (m.getSketch().isZeroWidth()) { // not sure about this if
//                    averagePrecision = 0;
//                }
                accuracy.put(m.getMonitorPoint(), averagePrecision);
            }
        }
        //scale down accuracies
        //find min accuracy
        double min = Double.MAX_VALUE;
        for (Double a : accuracy.values()) {
            min = Math.min(min, a);
        }
        if (min > lastAccuracy) {
            //scale down all accuracies
            for (Map.Entry<MonitorPoint, Double> entry : accuracy.entrySet()) {
                entry.setValue(1 - (1 - entry.getValue()) * min / lastAccuracy);
            }
        }
        for (Map.Entry<MonitorPoint, Double> entry : accuracy.entrySet()) {
            entry.setValue(Math.max(lastAccuracy, entry.getValue()));
        }
    }

    @Override
    public double getGlobalAccuracy() {
        return lastAccuracy;
    }

    @Override
    public void getUsedResources(Map<MonitorPoint, Integer> resource) {
        sketchMultiSwitch.getUsedResources(resource);
    }


    /**
     * DON'T use this for hashes and equals
     */
    public static class DynamicWildcardPattern extends WildcardPattern {

        public DynamicWildcardPattern(long data, int wildcardNum, double weight) {
            super(data, wildcardNum, weight);
        }

        public void setData(long d) {
            this.data = d;
        }

        public void setWildcardNum(int w) {
            this.wildcardNum = w;
        }
    }
}
