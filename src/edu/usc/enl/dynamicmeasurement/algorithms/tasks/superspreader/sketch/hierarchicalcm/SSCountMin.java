package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.SSAlgorithm;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import org.w3c.dom.Element;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 2:08 PM
 */
public class SSCountMin extends SSAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    private SSHierarchicalCountMin countMin;

    public SSCountMin(Element element) {
        super(element);
        countMin = new SSHierarchicalCountMin(element);
    }

    @Override
    public void reset() {
        countMin.reset();
    }

    @Override
    public void finish() {
        countMin.finish();
    }

    @Override
    public Collection<WildcardPattern> findSS() {
        return countMin.findHHH();
    }

    @Override
    public void doUpdate() {
        countMin.doUpdate();
    }

    @Override
    public void setFolder(String folder) {
        countMin.setFolder(folder);
    }

    @Override
    public void match(long key, long item) {
        countMin.match(key, item);
    }

    @Override
    public void setCapacityShare(int resource) {
        countMin.setCapacityShare(resource);
    }

    @Override
    public double estimateAccuracy() {
        return countMin.estimateAccuracy();
    }

    @Override
    public void setStep(int step) {
        super.setStep(step);
        countMin.setStep(step);
    }

    @Override
    public int getUsedResourceShare() {
        return countMin.getUsedResourceShare();
    }


}


