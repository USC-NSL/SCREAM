package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchHHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchSketch2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.MonitorPointData;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.SketchMultiSwitch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.SSAlgorithm;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/8/2014
 * Time: 5:37 PM
 */
public class SSCountMinMultiSwitch3 extends SSAlgorithm implements MultiSwitchTask.MultiSwitchTaskImplementation {
    protected MultiSwitchSketch3 countMinSketch;
    private final Element cardinalityCounterElement;

    public SSCountMinMultiSwitch3(Element element) {
        super(element);
        Map<String, Element> properties = Util.getChildrenProperties(element, "Property");
        cardinalityCounterElement = properties.get("CardinalityCounter");
        countMinSketch = new MultiSwitchSketch3(element);
    }

    @Override
    public Collection<WildcardPattern> findSS() {
        return countMinSketch.findHHH();
    }

    @Override
    public void doUpdate() {

    }

    public int getCCSize() {
        return SSHierarchicalCountMin.getCCSize(cardinalityCounterElement);
    }

    @Override
    public void match(long key, long item) {
        countMinSketch.match(key, item);
    }

    @Override
    public void setStep(int step) {
        super.setStep(step);
        countMinSketch.setStep(step);
    }

    @Override
    public void setFolder(String folder) {
        countMinSketch.setFolder(folder);
    }

    @Override
    public void reset() {
        countMinSketch.reset();
    }

    @Override
    public void finish() {
        countMinSketch.finish();
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

    protected class MultiSwitchSketch3 extends MultiSwitchSketch2 {

        private final NormalDistribution normalDistribution = new NormalDistribution();
        private final CardinalityCounter[] ccfc;
        private final CardinalityCounter[][] ccfc2;
        private final int[] indexArray;


        protected MultiSwitchSketch3(Element element) {
            super(element);
            ccfc = new CardinalityCounter[depth];
            ccfc2 = new CardinalityCounter[levels][depth];
            try {
                for (int i = 0; i < ccfc2.length; i++) {
                    CardinalityCounter[] ccs = ccfc2[i];
                    for (int j = 0; j < depth; j++) {
                        ccs[j] = (CardinalityCounter) Class.forName(cardinalityCounterElement.
                                getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class, Long.TYPE).newInstance(cardinalityCounterElement, (31 * (i + 1) + (j + 1)) * CardinalityCounter.SEED_CONST);
                    }
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            indexArray = new int[depth];
        }

        @Override
        protected int computeU(int i) {
            return WildcardPattern.TOTAL_LENGTH - wildcardNum;
        }

        @Override
        protected boolean filterLevel(int level) {
            return level == 0;
        }

        @Override
        protected void init(Collection<MonitorPoint> monitorPoints) {
            gran = 1;
            depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
            U = computeU(0);
            this.levels = (int) Math.ceil(1.0 * (getTaskWildcardPattern().getWildcardNum() - (WildcardPattern.TOTAL_LENGTH - U)) / gran);

            Map<MonitorPoint, AbstractHierarchicalCountMinSketch> map = new HashMap<>();
            monitorPoints.stream().filter(monitorPoint -> monitorPoint.hasDataFrom(this.getTaskWildcardPattern())).forEach(monitorPoint -> {
                map.put(monitorPoint, new SSHierarchicalCountMin(threshold, getTaskWildcardPattern(),
                        depth, U, gran, null, null, monitorPoint.getCapacity(), cardinalityCounterElement, SSHierarchicalCountMin.getCCSize(cardinalityCounterElement)));
            });
            this.sketchMultiSwitch = new SketchMultiSwitch(map);
        }

        @Override
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

            for (MonitorPointData monitorPointData : sketchMultiSwitch.getMonitorPoints()) {
                if (monitorPointData.getMonitorPoint().hasDataFrom(tempWildcardPattern)) {
                    tempContributingMonitors.add(monitorPointData);
                }
            }
            if (tempContributingMonitors.size() == 1) {
                return tempContributingMonitors.get(0).getSketch().CMH_count(level, item);
            }

            for (CardinalityCounter cardinalityCounter : ccfc2[level]) {
                cardinalityCounter.reset();
            }
            for (MonitorPointData monitorPointData : tempContributingMonitors) {
                SSHierarchicalCountMin sketch = getSSSketch(monitorPointData);
                sketch.CMH_count(level, item, ccfc);
                for (int i = 0; i < depth; i++) {
                    ccfc2[level][i].add(ccfc[i]);
                }
            }


            for (int i = 0; i < depth; i++) {
                counts[i] = ccfc2[level][i].getCardinality();
                indexArray[i] = i;
            }

            //find median
            SSHierarchicalCountMin.sort(counts, indexArray);
            long output;
            if (depth % 2 == 0) {
                output = (long) (counts[indexArray[depth / 2 - 1]] + counts[indexArray[depth / 2]]) / 2;
            } else {
                output = (long) counts[indexArray[depth / 2]];
            }

            //unbias

            double tempMean = 0;
            for (MonitorPointData monitorPointData : tempContributingMonitors) {
                SSHierarchicalCountMin ssSketch = getSSSketch(monitorPointData);
                if (!ssSketch.isExactCounter(level)) {
                    tempMean += (ssSketch.getSum() - ssSketch.hhSum) / ssSketch.getCapWidth(); //this is not exact
                }
            }

            output -= tempMean;

            return output;
        }

        protected SSHierarchicalCountMin getSSSketch(MonitorPointData monitorPointData) {
            return (SSHierarchicalCountMin) monitorPointData.getSketch();
        }

        protected double multiswitchPrecision(MultiSwitchHHHOutput hhh, IntegerWrapper chernoffFalseScale, int count, double mu, double maxS, int level) {
            //no need for chernoff
            double tempSum = 0;
            for (MonitorPointData monitorPointData : hhh.getContributingMonitors().keySet()) {
                SSHierarchicalCountMin ssSketch = getSSSketch(monitorPointData);
                tempSum += (ssSketch.getSum() - ssSketch.hhSum) / ssSketch.getCapWidth();
                ;
                //need to recompute this instead of mu_is because this value is removed from the item
                // not the new hhsum
            }

            double threshold2 =
                    //threshold;
                    threshold + tempSum;
            double CMMean = mu / threshold2;
            double falseProbability = 1;
            int[] allCounters = hhh.getAllCounters();
//        for (int i = 0; i < allCounters.length / 2 + 1; i++) { // no need for this, it is better to look at all counters
            for (int counter : allCounters) {
                int weight = counter;
//            if (weight > threshold) {
                //It is hard to find bad counters at each switch here
                double v = 0;
                //badCounters == null ? 0 : (badCounters[counter] - hhhOutput.getWildcardPattern().getWeight());
                // bad counters is the weight of all hhs - my weight it will be other hhs have collision on this counter
                double margin = (weight - threshold2 - v) / threshold2;
                if (margin > 0) { //still <0 guys can help
                    falseProbability *= Math.min(1, SSHierarchicalCountMin.computeFalseIntegrate2(margin, CMMean, ccfc2[0][0].getRelativeStd(), normalDistribution));
                } else {
                    falseProbability *= Math.min(1, 0.5 + SSHierarchicalCountMin.computeFalseIntegrate2(margin, CMMean, ccfc2[0][0].getRelativeStd(), normalDistribution));
                }

//            }
            }
            double precision = 1 - falseProbability;

            if (hhh.getDescendantHHHs().size() > 0) {
                precision = updateInternalHHHAccuracy(hhh, level, precision);

            } else {
                distributePrecisionForFringe(level, precision, hhh);
            }
            return precision;
        }

    }

}
