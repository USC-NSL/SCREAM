package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.MonitorPointData;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/25/13
 * Time: 5:03 PM
 */
public class MultiSwitchHHHOutput extends HHHOutput {
    private Map<MonitorPointData, Double> contributingMonitors;
    private double maxS;
    private double mu;

    public MultiSwitchHHHOutput(WildcardPattern wildcardPattern) {
        super(wildcardPattern);
    }

    public double getMaxS() {
        return maxS;
    }

    public void setMaxS(double maxS) {
        this.maxS = maxS;
    }

    public double getMu() {
        return mu;
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    public boolean isFromOneMonitorPoint() {
        return contributingMonitors.size() == 1;
    }

    public Map<MonitorPointData, Double> getContributingMonitors() {
        return contributingMonitors;
    }

    public void setContributingMonitors(Map<MonitorPointData, Double> contributingMonitors) {
        this.contributingMonitors = contributingMonitors;
    }

    //    protected MultiSwitchSketch2.MonitorPointData getOneMonitorPoint() {
//        return oneMonitorPoint;
//    }

//    protected void setOneMonitorPoint(MultiSwitchSketch2.MonitorPointData oneMonitorPoint) {
//        this.oneMonitorPoint = oneMonitorPoint;
//    }
}
