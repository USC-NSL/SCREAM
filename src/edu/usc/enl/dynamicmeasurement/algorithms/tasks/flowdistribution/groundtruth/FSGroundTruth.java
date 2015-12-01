package edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution.groundtruth;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution.FSAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 1:05 PM  <br/>
 * Finds the ground-truth flow size distribution by keeping track of the size of all items.
 */
public class FSGroundTruth extends FSAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    private Map<Long, Long> keySizeMap;

    public FSGroundTruth(Element element) {
        super(element);
        keySizeMap = new HashMap<>();
    }

    @Override
    public void reset() {
        keySizeMap.clear();
    }

    @Override
    public void finish() {

    }

    @Override
    public Map<Long, Long> findFS() {
        return keySizeMap.entrySet().stream().collect(Collectors.groupingBy(Map.Entry<Long, Long>::getValue, Collectors.counting()));
    }

    @Override
    public void doUpdate() {

    }

    @Override
    public void setFolder(String folder) {

    }

    @Override
    public void match(long key, long size) {
        Long items = keySizeMap.get(key);
        if (items == null) {
            items = 0l;
        }
        items += size;
        keySizeMap.put(key, items);
    }

    @Override
    public void setCapacityShare(int resource) {

    }

    @Override
    public double estimateAccuracy() {
        return 1;
    }

    @Override
    public int getUsedResourceShare() {
        return 1;
    }
}
