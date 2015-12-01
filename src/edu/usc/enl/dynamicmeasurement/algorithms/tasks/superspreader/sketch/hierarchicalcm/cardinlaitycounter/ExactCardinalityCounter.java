package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm.cardinlaitycounter;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm.CardinalityCounter;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/7/2014
 * Time: 2:10 PM
 */
public class ExactCardinalityCounter implements CardinalityCounter {
    private Set<Long> set = new HashSet<>();

    public ExactCardinalityCounter(Element element) {
    }

    public ExactCardinalityCounter() {

    }

    @Override
    public String toString() {
        return "ExactCardinalityCounter(" + set.size() + ")";
    }

    @Override
    public void match(long item) {
        set.add(item);
    }

    @Override
    public int getCardinality() {
        return set.size();
    }

    @Override
    public void reset() {
        set.clear();
    }

    @Override
    public void add(CardinalityCounter cc) {
        set.addAll(((ExactCardinalityCounter) cc).set);
    }

    @Override
    public double getRelativeStd() {
        return 0;
    }

    @Override
    public int getSize() {
        return 1;
    }
}
