package edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.stableskew;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.EntropyAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.multiswitch.separateallocation.MultiSwitchTask;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 1/5/14
 * Time: 7:17 AM
 * <br/>
 * Finds entropy for the traffic on multiple switches.
 * It uses the stableskew random variables and projection approach.
 * It assumes equal sized arrays on different switches, thus aggregating the data from multiple switches is
 * as simple as summing the counters.
 * TODO: Needs to be fixed to respect allocation and estimate accuracy
 */
public class MultiSwitchStableSkewJ extends EntropyAlgorithm implements MultiSwitchTask.MultiSwitchTaskImplementation {
    public int k = 100;
    public double alpha = 0.9999;
    protected Map<MonitorPoint, MonitorPointData> monitorPoints;

    public MultiSwitchStableSkewJ(Element element) {
        super(element);
        Map<String, Element> childrenProperties = Util.getChildrenProperties(element, "Property");
        k = Integer.parseInt(childrenProperties.get("k").getAttribute(ConfigReader.PROPERTY_VALUE));
        alpha = Double.parseDouble(childrenProperties.get("alpha").getAttribute(ConfigReader.PROPERTY_VALUE));
        Collection<MonitorPoint> monitorPoints = Util.getNetwork().getMonitorPoints();
        this.monitorPoints = new HashMap<>();
        for (MonitorPoint monitorPoint : monitorPoints) {
            if (monitorPoint.hasDataFrom(getTaskWildcardPattern())) {
                this.monitorPoints.put(monitorPoint, new MonitorPointData(new StableSkewJ(element), monitorPoint));
            }
        }
    }

    @Override
    public void match(long item, double diff) {
        for (MonitorPointData monitorPoint : monitorPoints.values()) {
            if (monitorPoint.getMonitorPoint().hasDataFrom(item)) {
                monitorPoint.getStableSkewJ().match(item, diff);
                break;
            }
        }
    }

    public double findEntropy() {
        double[] counters = new double[k];
        double sum = 0;
        //sum counters
        for (MonitorPointData monitorPointData : monitorPoints.values()) {
            double[] subCounters = monitorPointData.getStableSkewJ().getCounters();
            sum += monitorPointData.getStableSkewJ().getSum();
            for (int i = 0; i < counters.length; i++) {
                counters[i] += subCounters[i];
            }
        }
        // same algorithm as stableskew class
        double jHat = 0;
        double delta = 1 - alpha;
        double power = -Math.round(alpha / delta);
        for (double counter : counters) {
            jHat += Math.pow(counter / sum, power);
        }
        jHat = jHat * delta / counters.length;
        return -(Math.log(jHat));
    }

    @Override
    public void reset() {
        super.reset();
        for (MonitorPointData monitorPointData : monitorPoints.values()) {
            monitorPointData.reset();
        }
    }

    @Override
    public void setCapacityShare(Map<MonitorPoint, Integer> resource) {

    }

    @Override
    public void estimateAccuracy(Map<MonitorPoint, Double> accuracy) {
        for (Map.Entry<MonitorPoint, Double> entry : accuracy.entrySet()) {
            entry.setValue(1d);
        }
    }

    @Override
    public double getGlobalAccuracy() {
        return 1;
    }

    @Override
    public void getUsedResources(Map<MonitorPoint, Integer> resource) {

    }

    private static class MonitorPointData {
        private final StableSkewJ stableSkewJ;
        private final MonitorPoint monitorPoint;

        public MonitorPointData(StableSkewJ stableSkewJ, MonitorPoint monitorPoint) {
            this.stableSkewJ = stableSkewJ;
            this.monitorPoint = monitorPoint;
        }

        public StableSkewJ getStableSkewJ() {
            return stableSkewJ;
        }

        public MonitorPoint getMonitorPoint() {
            return monitorPoint;
        }

        public void reset() {
            stableSkewJ.reset();
        }
    }

}
