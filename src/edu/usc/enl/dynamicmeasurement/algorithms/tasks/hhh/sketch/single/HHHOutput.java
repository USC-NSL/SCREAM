package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/8/13
 * Time: 5:30 PM
 */
public class HHHOutput {
    private WildcardPattern wildcardPattern;
    private HHHSet descendantHHHs;
    private int[] allCounters;
    private int minIndex;

    public HHHOutput(WildcardPattern wildcardPattern) {
        this.wildcardPattern = wildcardPattern;
    }

    public HHHSet getDescendantHHHs() {
        return descendantHHHs;
    }

    public void setDescendantHHHs(HHHSet descendantHHHs) {
        this.descendantHHHs = descendantHHHs;
    }

    public WildcardPattern getWildcardPattern() {
        return wildcardPattern;
    }

    public void setAllCounters(int[] allCounters) {
        this.allCounters = allCounters;
    }

    public int[] getAllCounters() {
        return allCounters;
    }

    public void setMinIndex(int minIndex) {
        this.minIndex = minIndex;
    }

    public int getMinIndex() {
        return minIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HHHOutput hhhOutput = (HHHOutput) o;

        if (wildcardPattern != null ? !wildcardPattern.equals(hhhOutput.wildcardPattern) : hhhOutput.wildcardPattern != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return wildcardPattern != null ? wildcardPattern.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "HHHOutput{" +
                "wildcardPattern=" + wildcardPattern +
                ", descendantHHHs=" + descendantHHHs +
                '}';
    }
}
