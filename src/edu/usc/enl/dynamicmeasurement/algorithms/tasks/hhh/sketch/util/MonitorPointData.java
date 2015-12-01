package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.monitorpoint.MonitorPoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/9/2014
 * Time: 5:07 PM
 */
public class MonitorPointData {
    private final AbstractHierarchicalCountMinSketch sketch;
    //        private final IntegerWrapper indexWrapper;
    private final MonitorPoint monitorPoint;
    private double precision = 0;
    private Map<Integer, Set<HHHOutput>> levelHHH;
    private Map<Integer, Double> falseScales;
    private int count = 0;

    public MonitorPointData(AbstractHierarchicalCountMinSketch sketch, MonitorPoint monitorPoint) {
        this.sketch = sketch;
        this.monitorPoint = monitorPoint;
//            indexWrapper = new IntegerWrapper(0);
        levelHHH = new HashMap<>();
        falseScales = new HashMap<>();
    }

    public Map<Integer, Double> getFalseScales() {
        return falseScales;
    }

    public void reset() {
        sketch.getPrecisionEstimationData().reset();
//            indexWrapper.setValue(0);
        sketch.reset();
        levelHHH.clear();
        falseScales.clear();
        precision = 0;
        count = 0;
    }

    @Override
    public String toString() {
        return "MonitorPointData{" +
                "monitorPoint=" + monitorPoint.getIntId() +
                '}';
    }

    public AbstractHierarchicalCountMinSketch getSketch() {
        return sketch;
    }

//        private IntegerWrapper getIndexWrapper() {
//            return indexWrapper;
//        }

    public MonitorPoint getMonitorPoint() {
        return monitorPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MonitorPointData that = (MonitorPointData) o;

        if (monitorPoint != null ? !monitorPoint.equals(that.monitorPoint) : that.monitorPoint != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return monitorPoint != null ? monitorPoint.hashCode() : 0;
    }

    public double getAveragePrecision() {
        if (count == 0) {
            return 1;
        }
        //else there is still room for improvement

        return precision / count;
    }


    public double getErrorBound(int level, HHHOutput hhhOutput2) {
        if (sketch.isZeroWidth()) {
            return sketch.getSum();
        }
        if (sketch.isExactCounter(level)) {
            return 0;
        }
        double newSum = sketch.getSum() - getLevelHhsum().get(level);
        if (newSum == 0) {
            newSum = sketch.getSum();
        }
        WildcardPattern wildcardPattern = hhhOutput2.getWildcardPattern();
        return Math.min(
                sketch.CMH_count(level, sketch.realToCounterInput(wildcardPattern.getData() << wildcardPattern.getWildcardNum()) >>> level),
                newSum / sketch.getCapWidth()
        );
    }

    public Map<Integer, Double> getLevelHhCollisionRatio() {
        return sketch.getPrecisionEstimationData().getLevelHhCollisionRatio();
    }

    public Map<Integer, Double> getLevelHhsum() {
        return sketch.getPrecisionEstimationData().getLevelHhsum();
    }

    public Map<Integer, Set<HHHOutput>> getLevelHHH() {
        return levelHHH;
    }

    public void addPrecision(double precision) {
        if (Double.isNaN(precision)) {
            System.err.println("Nan precision for sketch " + sketch + " on monitor " + monitorPoint.getIntId());
        }
        count++;
        this.precision += precision;
    }


}
