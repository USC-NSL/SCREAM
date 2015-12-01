package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh2d;

import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Masoud
 * Date: 9/2/2014
 * Time: 9:16 AM
 */
public class WildcardPatternND implements Comparable<WildcardPatternND> {
    private WildcardPattern[] patterns;

    public WildcardPatternND(long[] data, int[] wildcardNums, double weight) {
        patterns = new WildcardPattern[data.length];
        for (int i = 0; i < data.length; i++) {
            patterns[i] = new WildcardPattern(data[i], wildcardNums[i], weight);
        }
    }

    public WildcardPatternND(String line) {
        String[] split = line.split(",");
        patterns = new WildcardPattern[split.length - 1];
        for (int i = 0; i < split.length - 1; i++) {
            patterns[i] = new WildcardPattern(split[i], Double.parseDouble(split[split.length - 1]));
        }
    }

    public WildcardPattern getDim(int dim) {
        return patterns[dim];
    }

    public int getDimNum() {
        return patterns.length;
    }

    @Override
    public String toString() {
        return Arrays.toString(patterns);
    }

    public boolean match(WildcardPatternND wp) {
        if (wp.getDimNum() != getDimNum()) {
            return false;
        }
        for (int i = 0; i < patterns.length; i++) {
            WildcardPattern pattern = patterns[i];
            if (!pattern.match(wp.patterns[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean match(long item, int dim) {
        if (getDimNum() < dim) {
            return false;
        }
        return patterns[dim].match(item);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WildcardPatternND)) return false;

        WildcardPatternND that = (WildcardPatternND) o;

        if (!Arrays.equals(patterns, that.patterns)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return patterns != null ? Arrays.hashCode(patterns) : 0;
    }

    public String toStringNoWeight() {
        StringBuilder sb = new StringBuilder();
        for (WildcardPattern pattern : patterns) {
            sb.append(pattern.toStringNoWeight()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public double getWeight() {
        return patterns[0].getWeight();
    }

    @Override
    public int compareTo(WildcardPatternND o) {
        for (int i = 0; i < patterns.length; i++) {
            WildcardPattern pattern = patterns[i];
            int c = pattern.compareTo(o.patterns[i]);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    public String toCIDRString() {
        StringBuilder sb = new StringBuilder();
        for (WildcardPattern pattern : patterns) {
            sb.append(pattern.toCIDRString()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
