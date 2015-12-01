package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchHHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchSketch2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.PrecisionEstimationData;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.MonitorPointData;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.SketchMultiSwitch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.SSAlgorithm;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/9/2014
 * Time: 6:33 PM
 */
public class SSCountMinMultiSwitch2 extends SSAlgorithm implements MultiSwitchTask.MultiSwitchTaskImplementation {
    private SSCountMinUnion ssCountMinUnion;
    protected SketchMultiSwitch sketchMultiSwitch;
    protected Element cardinalityCounterElement;


    public SSCountMinMultiSwitch2(Element element) {
        super(element);
        Map<String, Element> properties = Util.getChildrenProperties(element, "Property");
        cardinalityCounterElement = properties.get("CardinalityCounter");
        int depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
        int gran = 1;
        int U = computeU(0);

        HashMap<MonitorPoint, AbstractHierarchicalCountMinSketch> map = makeMonitorPointMap(depth, gran, U);
        sketchMultiSwitch = new SketchMultiSwitch(map);
        ssCountMinUnion = new SSCountMinUnion(element);
    }

    public static double computeFalseIntegral(double margin, double std, double ccMu, double maxS, NormalDistribution normalDistribution) {
        int probGranularity = 10;
        double p = 0;
        double step = 2.0 * std / probGranularity;
        //w=(1+delta)*mu
        //note mu is average of error
        if (margin > 0) {
            for (int j = -probGranularity; j < 0; j++) {
                double t = step * (j + 0.5);
                double delta = (margin - t) / ccMu - 1;
                double v = delta < 0 ? 1 : Math.min(1, MultiSwitchSketch2.chernoff(ccMu, maxS, margin - t, 1));
                double bound = j == -probGranularity ? -100 : (step * j / std);
                double normalProbability = normalDistribution.probability(bound, step * (j + 1) / std);
                p += normalProbability * v;
            }
            step = margin / probGranularity;
            for (int j = 0; j < probGranularity; j++) {
                double t = step * (j + 0.5);
                double delta = (margin - t) / ccMu - 1;
                double v = delta < 0 ? 1 : Math.min(1, MultiSwitchSketch2.chernoff(ccMu, maxS, margin - t, 1));
                double normalProbability = normalDistribution.probability(step * j / std, step * (j + 1) / std);
                p += normalProbability * v;
            }
        } else {
            for (int j = 0; j < probGranularity; j++) {
                double t = step * (j + 0.5);
                double delta = (margin + t) / ccMu - 1;
                double v = delta < 0 ? 1 : Math.min(0, MultiSwitchSketch2.chernoff(ccMu, maxS, margin + t, 1));
                double bound = j + 1 == probGranularity ? 100 : (step * (j + 1) / std);
                double normalProbability = normalDistribution.probability(step * j / std, bound);
                p += normalProbability * v;
            }
        }
        return p;
    }

    protected HashMap<MonitorPoint, AbstractHierarchicalCountMinSketch> makeMonitorPointMap(int depth, int gran, int u) {
        HashMap<MonitorPoint, AbstractHierarchicalCountMinSketch> map = new HashMap<>();
        Collection<MonitorPoint> monitorPoints = Util.getNetwork().getMonitorPoints();
        monitorPoints.stream().filter(monitorPoint -> monitorPoint.hasDataFrom(getTaskWildcardPattern())).forEach(monitorPoint -> {
            SSHierarchicalCountMin sketch = new SSHierarchicalCountMin(threshold, getTaskWildcardPattern(),
                    depth, u, gran, null, null, monitorPoint.getCapacity(), cardinalityCounterElement, SSHierarchicalCountMin.getCCSize(cardinalityCounterElement));
            map.put(monitorPoint, sketch);
            sketch.setCapacityShare(512);//just a small number to make it zero width
        });
        return map;
    }

    protected int computeU(int i) {
        return WildcardPattern.TOTAL_LENGTH - wildcardNum;
    }

    @Override
    public void setCapacityShare(Map<MonitorPoint, Integer> resource) {
        sketchMultiSwitch.setCapacityShare(resource);
    }

    @Override
    public void estimateAccuracy(Map<MonitorPoint, Double> accuracy) {
        //copied from multiswitchsketch2
//        Collection<MonitorPointData> monitorPoints = sketchMultiSwitch.getMonitorPoints();
//        if (cannotTraverseAll) {//assume only the root has been detected and is a wrong hhh
//            int n = monitorPoints.size();
//            double[] errorBounds = new double[n];
//            int i = 0;
//            double errorSum = 0;
//            for (MonitorPointData monitorPointData : monitorPoints) {
//                double errorBound = 0;
//                AbstractHierarchicalCountMinSketch sketch = monitorPointData.getSketch();
//                if (sketch.isZeroWidth()) {
//                    errorBound = sketch.getSum();
//                } else {
//                    errorBound = sketch.getSum() / sketch.getCapWidth();
//                }
//                errorBounds[i++] = errorBound;
//                errorSum += errorBound;
//            }
//            i = 0;
//            for (MonitorPointData monitorPointData : monitorPoints) {
//                double x = errorBounds[i++] / errorSum;
//                if (x >= 1.0 / n) {
//                    accuracy.put(monitorPointData.getMonitorPoint(), 0d);
//                } else {
//                    accuracy.put(monitorPointData.getMonitorPoint(), x * (0 - 1) + 1); ////linear from 1 to precision from 0 to 1/n
//                }
//            }
//        } else {
//            for (MonitorPointData m : monitorPoints) {
//                double averagePrecision = m.getAveragePrecision();
////                if (m.getSketch().isZeroWidth()) { // not sure about this if
////                    averagePrecision = 0;
////                }
//                accuracy.put(m.getMonitorPoint(), averagePrecision);
//            }
//        }
//        //scale down accuracies
//        //find min accuracy
//        double min = Double.MAX_VALUE;
//        for (Double a : accuracy.values()) {
//            min = Math.min(min, a);
//        }
//        if (min > lastAccuracy) {
//            //scale down all accuracies
//            for (Map.Entry<MonitorPoint, Double> entry : accuracy.entrySet()) {
//                entry.setValue(1 - (1 - entry.getValue()) * min / lastAccuracy);
//            }
//        }
//        for (Map.Entry<MonitorPoint, Double> entry : accuracy.entrySet()) {
//            entry.setValue(Math.max(lastAccuracy, entry.getValue()));
//        }
    }

    @Override
    public void setStep(int step) {
        super.setStep(step);
        ssCountMinUnion.setStep(step);
        sketchMultiSwitch.setStep(step);
    }

    @Override
    public double getGlobalAccuracy() {
        return ssCountMinUnion.getLastAccuracy();
    }

    @Override
    public void getUsedResources(Map<MonitorPoint, Integer> resource) {
        sketchMultiSwitch.getUsedResources(resource);
    }

    @Override
    public void reset() {
        ssCountMinUnion.reset();
        sketchMultiSwitch.reset();
    }

    @Override
    public void finish() {
        ssCountMinUnion.finish();
        //sketchMultiSwitch.finish();
    }

    @Override
    public Collection<WildcardPattern> findSS() {
        return ssCountMinUnion.findHHH();
    }

    @Override
    public void doUpdate() {
        ssCountMinUnion.doUpdate();
    }

    @Override
    public void setFolder(String folder) {
        ssCountMinUnion.setFolder(folder);
    }

    @Override
    public void match(long key, long item) {
        sketchMultiSwitch.match(key, item);
    }

    private class SSCountMinUnion extends SSHierarchicalCountMin {

        protected MultiSwitchSketch2.DynamicWildcardPattern tempWildcardPattern = new MultiSwitchSketch2.DynamicWildcardPattern(0, 0, 0);
        private List<MonitorPointData> tempContributingMonitors = new ArrayList<>();
        private final CardinalityCounter[] ccfc;
        private final int[] ccfmi;
        private final int[] ccfmie;
        private final Map<MonitorPointData, Long> tempEstimatedCardinality = new HashMap<>();
        private double tempMean;


        public SSCountMinUnion(Element element) {
            super(element);
            ccfc = new CardinalityCounter[depth];
            ccfmi = new int[depth];
            ccfmie = new int[depth];
        }

        public double getLastAccuracy() {
            return lastAccuracy;
        }

        public void prepareToFindHHH() {
            // prepare each of them
            super.prepareToFindHHH();
            System.out.println(getStep() + "," + lastAccuracy);
            hhSum = 1;
            sum = 0;
            for (MonitorPointData monitorPointData : sketchMultiSwitch.getMonitorPoints()) {
                SSHierarchicalCountMin sketch = getSSSketch(monitorPointData);
                sketch.prepareToFindHHH();
                hhSum += sketch.hhSum;
                sum += sketch.getSum();
            }
        }

        protected SSHierarchicalCountMin getSSSketch(MonitorPointData monitorPointData) {
            return (SSHierarchicalCountMin) monitorPointData.getSketch();
        }

        @Override
        protected PrecisionEstimationData MakePrecisionEstimationData() {
            return new MyRecallAndPrecisionEstimationData();
        }

        @Override
        protected HHHOutput getHhhOutput(WildcardPattern wildcardPattern) {
            //this only works for level 0
            int childLevel = getLevel(wildcardPattern.getWildcardNum());
            int count = 0;
            int[] allCounters = new int[depth];
            for (int i1 = 0, indexArrayLength = indexArray.length; i1 < indexArrayLength; i1++) {
                int i = indexArray[i1];
                if (countsCache[childLevel][i] > (threshold + tempMean)) {
                    count++;
                }
                allCounters[i1] = countsCache[childLevel][i];
            }
            SSOutputMultiSwitch output = new SSOutputMultiSwitch(wildcardPattern, count);
            output.setAllCounters(allCounters);
            return output;
        }

        private class SSOutputMultiSwitch extends MultiSwitchHHHOutput {
            private final int count;

            public SSOutputMultiSwitch(WildcardPattern wildcardPattern, int count) {
                super(wildcardPattern);
                this.count = count;
            }

            public int getCount() {
                return count;
            }
        }


        public long CMH_count(int level, long item, IntegerWrapper indexW, int[] indexArray) {
            if (level >= levels) {
                return (sum);
            }

            int shifts = getRealWildcardNums(level);
            tempWildcardPattern.setData(getRealData(item, shifts));
            tempWildcardPattern.setWildcardNum(shifts);
            tempContributingMonitors.clear();
            sketchMultiSwitch.getMonitorPoints().stream().filter(monitorPoint -> monitorPoint.getMonitorPoint().hasDataFrom(tempWildcardPattern)).forEach(tempContributingMonitors::add);
            if (tempContributingMonitors.size() == 1) {
                return getSSSketch(tempContributingMonitors.get(0)).CMH_count(level, item);
            }

            //if one of them is still exact, fall back to sum of estimates |D1|+|D2|. this will overestimate but who cares as long as this is not the last level!
            boolean hasExactLevel = false;
            for (MonitorPointData monitorPoint : tempContributingMonitors) {
                if (getSSSketch(monitorPoint).isExactCounter(level)) {
                    hasExactLevel = true;
                    break;
                }
            }
            if (hasExactLevel || level > wildcardNum) {
                if (level == wildcardNum) {
                    throw new RuntimeException("Exact counter for level 0");
                }
                long sum = 0;
                for (MonitorPointData monitorPointData : tempContributingMonitors) {
                    sum += monitorPointData.getSketch().CMH_count(level, item);
                }
                return sum;
            } else {

                // return an estimate of item at level depth
                // find monitor points that match item

                for (int i = 0; i < depth; i++) {
                    counts[level][i].reset();
                }
                tempEstimatedCardinality.clear();
                for (MonitorPointData monitorPoint : tempContributingMonitors) {
                    // find counters for item
                    SSHierarchicalCountMin sketch = getSSSketch(monitorPoint);
                    sketch.CMH_count(level, item, ccfc);
                    for (int i = 0; i < ccfc.length; i++) {
                        counts[level][i].add(ccfc[i]);
                        indexArray[i] = i;
                        ccfmie[i] = ccfc[i].getCardinality();
                    }
                    tempEstimatedCardinality.put(monitorPoint, sketch.estimateMedian(level, indexArray, ccfmie));
                }

                for (int i = 0; i < depth; i++) {
                    ccfmi[i] = counts[level][i].getCardinality();
                    indexArray[i] = i;
                }

                //find median
                //index array and index are not important
                sort(ccfmi, indexArray);

                long output;
                if (depth % 2 == 0) {
                    output = (long) (ccfmi[indexArray[depth / 2 - 1]] + ccfmi[indexArray[depth / 2]]) / 2;
                } else {
                    output = (long) ccfmi[indexArray[depth / 2]];
                }

                tempMean = 0;
                for (MonitorPointData monitorPointData : tempContributingMonitors) {
                    SSHierarchicalCountMin ssSketch = getSSSketch(monitorPointData);
                    tempMean += (ssSketch.getSum() - ssSketch.hhSum - tempEstimatedCardinality.get(monitorPointData)) / ssSketch.getCapWidth(); //this is not exact
                }

                output -= tempMean;

                return Math.max(0, output);
            }
        }

        private class MyRecallAndPrecisionEstimationData extends RecallAndPrecisionEstimationData {
            IntegerWrapper chernoffFalseScale = new IntegerWrapper(0);
            int count;

            public MyRecallAndPrecisionEstimationData() {
                super(SSCountMinUnion.this, false);
            }

            public void updateAccuracy(Map<Integer, List<HHHOutput>> result, int outputSize) {
                sketchMultiSwitch.setMonitorPointsItems(result);
                Map<Integer, List<HHHOutput>> result2 = new HashMap<>();
                for (Map.Entry<Integer, List<HHHOutput>> entry : result.entrySet()) {
                    List<HHHOutput> l = new ArrayList<>(entry.getValue().size());
                    for (HHHOutput hhhOutput : entry.getValue()) {
                        MultiSwitchHHHOutput hhh = (MultiSwitchHHHOutput) hhhOutput;
                        Map<MonitorPointData, Double> contributingMonitors = new HashMap<>();
                        double mu = 0;
                        double maxS = 0;
                        for (MonitorPointData monitorPoint : sketchMultiSwitch.getMonitorPoints()) {
                            if (monitorPoint.getMonitorPoint().hasDataFrom(hhh.getWildcardPattern())) {
                                double mu_i;
                                AbstractHierarchicalCountMinSketch sketch = monitorPoint.getSketch();

                                double tailSum = sketch.getSum();
                                if (sketch.isZeroWidth()) {
                                    mu_i = tailSum;
                                } else {
                                    Double aDouble = monitorPoint.getLevelHhsum().get(0);
                                    tailSum -= aDouble == null ? 0 : aDouble;
                                    mu_i = tailSum / sketch.getCapWidth();
                                }
                                maxS = Math.max(tailSum, maxS);
                                contributingMonitors.put(monitorPoint, mu_i);
                                mu += mu_i;
                            }
                        }
                        hhh.setMaxS(maxS);
                        hhh.setMu(mu);
                        hhh.setContributingMonitors(contributingMonitors);
                        l.add(hhh);
                    }
                    result2.put(entry.getKey(), l);
                }
                chernoffFalseScale.setValue(0);
                count = outputSize;
                levelHhsum.put(0, hhSum * 1.0);
                super.updateAccuracy(result2, outputSize);
            }

            @Override
            public void fixLevelHHSum() {
                // each sketch must do hhfinder
                // find false scales
                sketchMultiSwitch.prepareForAccuracyEstimation();
            }

            protected double getLevelAccuracy(List<HHHOutput> levelHHHs, int level) {
                if (levelHHHs == null || levelHHHs.size() == 0) {
                    return 0;
                }
                //now for each hhh find its precision
                double precision = 0;
                for (HHHOutput hhh : levelHHHs) {
                    precision += getHHHAccuracy(1, hhh, level); //I don't have false scale
                }
                return precision;
            }

            public double getAFringeHHHAccuracy(double falseScale, HHHOutput hhh, int level) {
                MultiSwitchHHHOutput multiSwitchHhhOutput = (MultiSwitchHHHOutput) hhh;
                double precision;
                double ccSigma = getCCSigma() * threshold; //it is relative so no need to multiply by number of contributing switches
                if (multiSwitchHhhOutput.getContributingMonitors().size() == 1) {
                    MonitorPointData monitorPointData = multiSwitchHhhOutput.getContributingMonitors().keySet().iterator().next();
                    PrecisionEstimationData precisionEstimationData1 = monitorPointData.getSketch().getPrecisionEstimationData();
//                    Double falseScale1 = monitorPointData.getFalseScales().get(level);
                    precision = precisionEstimationData1.getAFringeHHHAccuracy(1, hhh, level);
                    monitorPointData.addPrecision(precision);
                } else {
                    double maxS = multiSwitchHhhOutput.getMaxS();
                    if (maxS <= 0) {
                        //there is no cm error
                        precision = normalDistribution.cumulativeProbability((hhh.getWildcardPattern().getWeight() - multiSwitchHhhOutput.getMu()) / ccSigma);
                    } else {
                        double falseProbability = 1;
                        for (int c : hhh.getAllCounters()) {
                            double margin = (c - threshold);
                            falseProbability *= computeFalseIntegral(margin, ccSigma, multiSwitchHhhOutput.getMu(), maxS, normalDistribution);
                        }
                        precision = 1 - falseProbability;
//                                Math.pow(computeFalseIntegral(margin, ccSigma, multiSwitchHhhOutput.getMu(), maxS, normalDistribution),
//                                ((SSOutputMultiSwitch) multiSwitchHhhOutput).getCount());
                        if (precision < 0.1) {
                            //I want falsescale to be f/n which is sum(1+2(i-1))/n for i=1 to f (as a streaming algorithm)
                            chernoffFalseScale.add(1);
//                            double falseProbability = 1 - precision;
                            precision = Math.max(0, (1 - falseProbability * (1 + 2 * (chernoffFalseScale.getValue())) / count));
                        }
                    }
                    distributeOnSwitches(multiSwitchHhhOutput, precision);
                }
                return precision;
            }

            private void distributeOnSwitches(MultiSwitchHHHOutput hhh, double precision) {
                Collection<MonitorPointData> contributingMonitors = hhh.getContributingMonitors().keySet();
                double errorSum = 0;
                int level = 0;
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
        }
    }
}
