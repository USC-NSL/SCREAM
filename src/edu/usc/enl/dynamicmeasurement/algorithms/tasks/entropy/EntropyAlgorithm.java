package edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy;

import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 1/4/14
 * Time: 8:38 PM <br/>
 * Implements an algorithm for finding the entropy of a map of items with sizes.
 * The definition of entropy will be stated by the concrete class if it is not Shannon Entropy
 * <p>The XML constructor requires the following Property children tags: <ul>
 * <li> name attribute as "Filter", value attribute as a prefix pattern</li>
 * </ul></p>
 */
public abstract class EntropyAlgorithm {
    protected final WildcardPattern taskWildcardPattern;

    protected EntropyAlgorithm(Element element) {
        Map<String, Element> childrenProperties = Util.getChildrenProperties(element, "Property");
        this.taskWildcardPattern = new WildcardPattern(childrenProperties.get("Filter").getAttribute(ConfigReader.PROPERTY_VALUE), 0);
    }

    public WildcardPattern getTaskWildcardPattern() {
        return taskWildcardPattern;
    }

    public abstract void match(long item, double diff);

    /**
     * @return a positive number as the entropy
     */
    public abstract double findEntropy();

    public void reset() {

    }

    public String toString() {
        return getClass().getSimpleName();
    }

    public void finish(FinishPacket p) {

    }

    public void setFolder(String folder) {

    }
}
