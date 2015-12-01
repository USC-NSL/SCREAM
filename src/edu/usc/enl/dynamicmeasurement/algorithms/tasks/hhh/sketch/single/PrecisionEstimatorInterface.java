package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/10/2014
 * Time: 6:20 AM
 */
public interface PrecisionEstimatorInterface {
    public void updateAccuracy(Map<Integer, List<HHHOutput>> result, int outputSize);

    public void reset();
}
