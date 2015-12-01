package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 1:06 PM
 */
public abstract class SSAlgorithm implements Task2.TaskImplementation {
    private int step = 0;
    protected final WildcardPattern taskWildcardPattern;
    protected final double threshold;
    protected final int wildcardNum;

    public SSAlgorithm(Element element) {
        Map<String, Element> childrenProperties = Util.getChildrenProperties(element, "Property");
        this.threshold = Double.parseDouble(childrenProperties.get("Threshold").getAttribute(ConfigReader.PROPERTY_VALUE));
        this.taskWildcardPattern = new WildcardPattern(childrenProperties.get("Filter").getAttribute(ConfigReader.PROPERTY_VALUE), 0);
        this.wildcardNum = Integer.parseInt(childrenProperties.get("WildcardNum").getAttribute(ConfigReader.PROPERTY_VALUE));
    }

    public abstract void reset();

    public abstract void finish();

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public abstract Collection<WildcardPattern> findSS();

    public abstract void doUpdate();

    public abstract void setFolder(String folder);

    public WildcardPattern getTaskWildcardPattern() {
        return taskWildcardPattern;
    }

    public double getThreshold() {
        return threshold;
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    public abstract void match(long key, long item);

}
