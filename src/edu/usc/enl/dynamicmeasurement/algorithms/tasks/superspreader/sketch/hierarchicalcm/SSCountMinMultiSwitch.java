package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchHHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch.MultiSwitchSketch2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/8/2014
 * Time: 5:37 PM
 */
public class SSCountMinMultiSwitch extends SSAlgorithm implements MultiSwitchTask.MultiSwitchTaskImplementation {
    protected MultiSwitchSketch3 countMinSketch;
    private final Element cardinalityCounterElement;

    public SSCountMinMultiSwitch(Element element) {
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

        protected double multiswitchPrecision(MultiSwitchHHHOutput hhh, IntegerWrapper chernoffFalseScale, int count, double mu, double maxS, int level) {
            double precision;
            hhh.setMaxS(maxS);
            hhh.setMu(mu);
            double falseProbability;
            double ccSigma = 0;
            double ccMu = threshold;
            for (MonitorPointData monitorPointData : hhh.getContributingMonitors().keySet()) {
                ccSigma += Math.pow(((SSHierarchicalCountMin) monitorPointData.getSketch()).getCCSigma(), 2);
            }
            ccSigma = Math.sqrt(ccSigma) * threshold;
            if (maxS <= 0) {
                //there is no cm error
                precision = normalDistribution.cumulativeProbability((hhh.getWildcardPattern().getWeight() - ccMu) / ccSigma);
            } else {
                double margin = (hhh.getWildcardPattern().getWeight() - threshold);
                precision = 1 - Math.pow(SSCountMinMultiSwitch2.computeFalseIntegral(margin, ccSigma, threshold, maxS, normalDistribution), depth / 2);
                if (precision < 0.1) {
                    //I want falsescale to be f/n which is sum(1+2(i-1))/n for i=1 to f (as a streaming algorithm)
                    chernoffFalseScale.add(1);
                    falseProbability = 1 - precision;
                    precision = Math.max(0, (1 - falseProbability * (1 + 2 * (chernoffFalseScale.getValue())) / count));
                }
            }

            if (hhh.getDescendantHHHs().size() > 0) {
                precision = updateInternalHHHAccuracy(hhh, level, precision);

            } else {
                distributePrecisionForFringe(level, precision, hhh);
            }
            return precision;
        }

    }

}
