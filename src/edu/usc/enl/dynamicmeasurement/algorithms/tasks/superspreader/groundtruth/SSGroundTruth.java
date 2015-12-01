package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.groundtruth;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.SSAlgorithm;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 1:05 PM
 */
public class SSGroundTruth extends SSAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    protected Map<Long, Set<Long>> keyItemMap;

    public SSGroundTruth(Element element) {
        super(element);
        keyItemMap = new HashMap<>();
    }

    @Override
    public void reset() {
        System.out.println(getStep() + "," + taskWildcardPattern.toStringNoWeight() + "," + keyItemMap.values().stream().mapToDouble(Set::size).sum());
        keyItemMap.clear();
    }

    @Override
    public void finish() {

    }

    @Override
    public Collection<WildcardPattern> findSS() {
//        System.out.print("y{" + (getStep() + 1) + "}=[");
//        keyItemMap.entrySet().stream().collect(Collectors.groupingBy(e -> e.getValue().size(), Collectors.counting())).
//                entrySet().stream().sorted((a, b) -> a.getKey() - b.getKey()).forEachOrdered(e -> System.out.print(e.getKey() + "," + e.getValue() + ";"));
//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++");
//        System.out.println("];");

        return keyItemMap.entrySet().stream().filter(e -> e.getValue().size() >= threshold).
                map(e -> new WildcardPattern(e.getKey(), 0, e.getValue().size())).collect(Collectors.toList());
    }

    @Override
    public void doUpdate() {

    }

    @Override
    public void setFolder(String folder) {

    }

    @Override
    public void match(long key, long item) {
        Set<Long> items = keyItemMap.get(key);
        if (items == null) {
            items = new HashSet<>();
            keyItemMap.put(key, items);
        }
        items.add(item);
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
