package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/7/2014
 * Time: 11:37 AM
 */
public interface CardinalityCounter {
    public long SEED_CONST = 129837593847l;

    public void match(long item);

    public int getCardinality();

    public void reset();

    public void add(CardinalityCounter cc);

    public double getRelativeStd();

    int getSize();
}
