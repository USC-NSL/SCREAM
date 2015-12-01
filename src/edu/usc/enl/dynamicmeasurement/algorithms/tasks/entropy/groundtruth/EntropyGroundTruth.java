package edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.groundtruth;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.EntropyAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.util.ControlledBufferWriter;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 1/4/14
 * Time: 8:37 PM  <br/>
 * Computes the Shannon entropy of the set of items by keeping track of the size of all items.
 */
public class EntropyGroundTruth extends EntropyAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    private Map<Long, Double> ipSizeMap = new HashMap<>();
    private ControlledBufferWriter numWriter;
    private int step = 0;

    public EntropyGroundTruth(Element element) {
        super(element);
    }

    @Override
    public void setCapacityShare(int resource) {

    }

    @Override
    public void setFolder(String folder) {
        super.setFolder(folder);
        try {
            numWriter = Util.getNewWriter(folder + "/num.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public double estimateAccuracy() {
        return 1;
    }

    @Override
    public int getUsedResourceShare() {
        return 0;
    }

    @Override
    public void match(long item, double diff) {
        Double aDouble = ipSizeMap.get(item);
        if (aDouble == null) {
            aDouble = 0d;
        }
        ipSizeMap.put(item, aDouble + diff);
    }

    @Override
    public double findEntropy() {
        double sum = 0;
        for (Double aDouble : ipSizeMap.values()) {
            sum += aDouble;
        }
        double output = 0;
        for (Double aDouble : ipSizeMap.values()) {
            output += aDouble / sum * Math.log(sum / aDouble);
        }
        numWriter.println(step + "," + ipSizeMap.size());
        step++;
        return output;
    }

    @Override
    public void reset() {
        super.reset();
        ipSizeMap.clear();
    }

    @Override
    public void finish(FinishPacket p) {
        super.finish(p);
        numWriter.close();
    }
}
