package edu.usc.enl.dynamicmeasurement.algorithms.tasks;

import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.data.DataPacket;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/2/13
 * Time: 4:00 PM <br/>
 * The main class that implements a task. It knows how to use the packets and create the report
 */
public abstract class TaskUser {
    protected final Method getKeyField;
    protected final Method setKeyField;

    public TaskUser(Element element) {
        Element keyFieldElement = Util.getChildrenProperties(element, "Property").get("KeyField");
        String keyField2 = "SrcIP";
        if (keyFieldElement != null) {
            keyField2 = keyFieldElement.getAttribute(ConfigReader.PROPERTY_VALUE);
        }
        Method tempGet = null;
        Method tempSet = null;
        try {
            tempGet = DataPacket.class.getMethod("get" + keyField2);
            tempSet = DataPacket.class.getMethod("set" + keyField2, Long.TYPE);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        getKeyField = tempGet;
        setKeyField = tempSet;
    }

    public abstract void setFolder(String folder);

    public abstract Task2.TaskImplementation getImplementation();

    /**
     * creates the report for the enduser
     *
     * @param step
     */
    public abstract void report(int step);

    /**
     * update the internal data structure of the algorithm. Keep this separate from the report part for the sake of profiling
     *
     * @param step
     */
    public abstract void update(int step);

    public abstract void process2(DataPacket p);

    public abstract void finish(FinishPacket p);

    public Method getGetKeyField() {
        return getKeyField;
    }
}
