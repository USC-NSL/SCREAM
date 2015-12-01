package edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 1:06 PM
 * <br/>
 * Implements an algorithm for finding the entropy of a map of items with sizes.
 * <p>The XML constructor requires the following Property children tags: <ul>
 * <li> name attribute as "Filter", value attribute as a prefix pattern</li>
 * </ul></p>
 */
public abstract class FSAlgorithm implements Task2.TaskImplementation {
    private int step = 0;
    protected final WildcardPattern taskWildcardPattern;

    public FSAlgorithm(Element element) {
        Map<String, Element> childrenProperties = Util.getChildrenProperties(element, "Property");
        this.taskWildcardPattern = new WildcardPattern(childrenProperties.get("Filter").getAttribute(ConfigReader.PROPERTY_VALUE), 0);
    }

    public abstract void reset();

    public abstract void finish();

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    /**
     * @return a map showing the distribution of size of items.
     * The keys may just be the aggregated keys and they may even not be in the input
     */
    public abstract Map<Long, Long> findFS();

    /**
     * update the internal data structures. Just separate it from whatever needed from creating the report and
     * estimating the accuracy
     */
    public abstract void doUpdate();

    public abstract void setFolder(String folder);

    public WildcardPattern getTaskWildcardPattern() {
        return taskWildcardPattern;
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    public abstract void match(long key, long size);
}
