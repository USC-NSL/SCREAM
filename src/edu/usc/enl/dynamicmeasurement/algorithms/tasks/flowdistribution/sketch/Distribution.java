package edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution.sketch;

import java.util.HashMap;
import java.util.Map;

/**
 * keeps track of a distribution
 */
public class Distribution {
    Map<Integer, Double> freq = new HashMap<>();

    public Distribution(Map<Integer, ? extends Number> freq) {
        for (Map.Entry<Integer, ? extends Number> entry : freq.entrySet()) {
            this.freq.put(entry.getKey(), entry.getValue().doubleValue());
        }
    }

    Distribution() {
    }

    @Override
    public String toString() {
        return toString(1);
    }

    public String toString(double scale) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Double> entry : freq.entrySet()) {
            sb.append(entry.getKey() + "," + ((int) (entry.getValue() * scale)) + ";");
        }
        return sb.toString();
    }

    public void fillFrom(Distribution newDistribution) {
        clear();
        freq.putAll(newDistribution.freq);
    }

    public void clear() {
        freq.clear();
    }

    public void addFreq(Integer key, double v) {
        Double value = freq.get(key);
        if (value == null) {
            value = 0d;
        }
        value += v;
        freq.put(key, value);
    }

    public double sumFreq() {
        return freq.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getFreq(int i) {
        Double frequency = freq.get(i);
        if (frequency == null) {
            return 0;
        }
        return frequency;
    }
}
