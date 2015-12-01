package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.multiswitch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.HHHOutput;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.MonitorPointData;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 12/13/13
 * Time: 6:57 PM
 */
public class MultiSwitchSketchGroundTruth extends MultiSwitchSketch2 {
    private final String groundTruthFolder;
    private Map<Integer, Set<WildcardPattern>> groundTruth = new HashMap<>();

    public MultiSwitchSketchGroundTruth(Element element) {
        super(element);
        Map<String, Element> childrenProperties = Util.getChildrenProperties(element, "Property");
        groundTruthFolder = childrenProperties.get("GroundTruthFolder").getAttribute(ConfigReader.PROPERTY_VALUE);
    }

    @Override
    public void setFolder(String folder) {
        super.setFolder(folder);
        String name = new File(folder).getName();
        try (BufferedReader br = new BufferedReader(new FileReader(groundTruthFolder + "/" + name + "/hhh.csv"))) {
            while (br.ready()) {
                String line = br.readLine();
                String[] split = line.split(",");
                WildcardPattern hhh = new WildcardPattern(split[1], Double.parseDouble(split[2]));
                int step = Integer.parseInt(split[0]);
                Set<WildcardPattern> wildcardPatterns = groundTruth.get(step);
                if (wildcardPatterns == null) {
                    wildcardPatterns = new HashSet<>();
                    groundTruth.put(step, wildcardPatterns);
                }
                wildcardPatterns.add(hhh);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected double getHHHAccuracy(MultiSwitchHHHOutput hhh, IntegerWrapper chernoffFalseScale, int count) {
        Set<WildcardPattern> wildcardPatterns = groundTruth.get(getStep());
        double precision;
        if (wildcardPatterns != null && wildcardPatterns.contains(hhh.getWildcardPattern())) {
            precision = 1;
        } else {
            precision = 0;
        }
        int level = getLevel(hhh.getWildcardPattern().getWildcardNum());
        Map<MonitorPointData, Double> contributingMonitors = new HashMap<>();
        double mu = 0;
        double maxS = -1;
        for (MonitorPointData monitorPoint : sketchMultiSwitch.getMonitorPoints()) {
            Map<Integer, Set<HHHOutput>> levelHHH = monitorPoint.getLevelHHH();
            if (levelHHH.containsKey(level) && levelHHH.get(level).contains(hhh)) {
                double mu_i;
                AbstractHierarchicalCountMinSketch sketch = monitorPoint.getSketch();
                if (sketch.isExactCounter(level)) {
                    if (sketch.isZeroWidth()) {
                        mu_i = sketch.getSum();
                    } else {
                        mu_i = 0;
                    }
                } else {
                    double tailSum = sketch.getSum();
                    if (sketch.isZeroWidth()) {
                        mu_i = tailSum;
                    } else {
                        tailSum -= monitorPoint.getLevelHhsum().get(level);
                        if (tailSum == 0) {
                            tailSum = sketch.getSum();
                        }
                        mu_i = tailSum / sketch.getCapWidth();
                    }
                    maxS = Math.max(tailSum, maxS);
                }
                contributingMonitors.put(monitorPoint, mu_i);
                mu += mu_i;
            }
        }

        hhh.setContributingMonitors(contributingMonitors);
        if (contributingMonitors.size() == 1) {
            MonitorPointData oneMonitorPoint = contributingMonitors.keySet().iterator().next();
            if (oneMonitorPoint.getSketch().isExactCounter(level) && precision == 0 && hhh.getDescendantHHHs().size() == 0) {
                System.out.println("false exact fringe hhh");
                System.exit(1);
            }
            oneMonitorPoint.addPrecision(precision);
        } else {
            hhh.setMu(mu);
            hhh.setMaxS(maxS);
            if (precision == 1) {
                for (MonitorPointData monitorPointData : contributingMonitors.keySet()) {
                    monitorPointData.addPrecision(1);
                }
            } else {
                //need to find whose fault it is
                if (hhh.getDescendantHHHs().size() > 0) {
                    double errorSum = 0;
                    for (MonitorPointData monitorPointData : contributingMonitors.keySet()) {
                        errorSum += monitorPointData.getErrorBound(level, hhh);
                    }
                    if (errorSum == 0) {
                        //the problem is ONLY because of descendants, use the formula of fringe but use descendants error for them
                        updateExactFalsePositives(hhh, precision, contributingMonitors, errorSum);

                    } else {
                        updateInternalHHHAccuracy(hhh, level, precision);
                    }
                } else {
                    distributePrecisionForFringe(level, precision, hhh);
                }
            }
        }
        return precision;
    }

    private void updateExactFalsePositives(MultiSwitchHHHOutput hhh, double precision, Map<MonitorPointData, Double> contributingMonitors, double errorSum) {
        Map<MonitorPointData, Double> contributingError = new HashMap<>();
        for (MonitorPointData monitorPointData : contributingMonitors.keySet()) {
            contributingError.put(monitorPointData, 0d);
        }
        for (Map.Entry<Integer, List<HHHOutput>> entry : hhh.getDescendantHHHs().getResult().entrySet()) {
            List<HHHOutput> hhhs = entry.getValue();
            for (HHHOutput descendantHHH2 : hhhs) {
                MultiSwitchHHHOutput descendantHHH = (MultiSwitchHHHOutput) descendantHHH2;
                if (descendantHHH.isFromOneMonitorPoint()) {
                    //if the descendant hhh matches only one switch find its p error bound based on markov
                    MonitorPointData monitorPointData = descendantHHH.getContributingMonitors().keySet().iterator().next();
                    int descendantLevel = getLevel(descendantHHH.getWildcardPattern().getWildcardNum());
                    if (!monitorPointData.getSketch().isExactCounter(descendantLevel)) {
                        double errorBound = computeDescendantOneMonitorErrorBound(DESCENDANT_ERROR_BOUND_P, monitorPointData, descendantLevel);
                        contributingError.put(monitorPointData, contributingError.get(monitorPointData) + errorBound);
                    }
                } else {
                    if (descendantHHH.getMaxS() > 0) { //zero is also good for special cases where all traffic comes from hhh
                        //else find its p error bound based on chernoff
                        double errorBound = computeDescendantMultiMonitorErrorBound(DESCENDANT_ERROR_BOUND_P, descendantHHH);
                        for (Map.Entry<MonitorPointData, Double> entry2 : descendantHHH.getContributingMonitors().entrySet()) {
                            contributingError.put(entry2.getKey(), contributingError.get(entry2.getKey()) + errorBound / descendantHHH.getMu() * entry2.getValue());
                        }
                    } else { //this was exact on all counters
                    }
                }
            }
        }

        for (Double error : contributingError.values()) {
            errorSum += error;
        }

        int n = contributingMonitors.size();
        for (MonitorPointData monitorPointData : contributingMonitors.keySet()) {
            double errorBound = contributingError.get(monitorPointData);
            double x = errorBound / errorSum;
            if (x >= 1.0 / n) {
                monitorPointData.addPrecision(precision);
            } else {
                monitorPointData.addPrecision(x * (precision - 1) + 1);//linear from 1 to precision from 0 to 1/n
            }
        }
    }
}
