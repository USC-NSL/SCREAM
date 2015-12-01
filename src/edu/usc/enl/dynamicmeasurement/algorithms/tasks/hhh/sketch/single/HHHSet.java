package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/8/13
 * Time: 5:30 PM
 */
public class HHHSet {
    private Map<Integer, List<HHHOutput>> result = new HashMap<>();
    private int size;

    public void add(HHHOutput output, int level) {
        size++;
        List<HHHOutput> hhhOutputs = result.get(level);
        if (hhhOutputs == null) {
            hhhOutputs = new LinkedList<>();
            result.put(level, hhhOutputs);
        }
        hhhOutputs.add(output);
    }

    public double size() {
        return size;
    }

    public void merge(HHHSet other) {
        for (Map.Entry<Integer, List<HHHOutput>> entry : other.result.entrySet()) {
            List<HHHOutput> hhhOutputs = result.get(entry.getKey());
            if (hhhOutputs == null) {
                result.put(entry.getKey(), new LinkedList<>(entry.getValue()));
            } else {
                hhhOutputs.addAll(entry.getValue());
            }
            size += entry.getValue().size();
        }
    }

    public Map<Integer, List<HHHOutput>> getResult() {
        return result;
    }
}
