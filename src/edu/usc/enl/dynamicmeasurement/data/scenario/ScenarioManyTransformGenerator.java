package edu.usc.enl.dynamicmeasurement.data.scenario;

import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.model.event.AddTaskEvent;
import edu.usc.enl.dynamicmeasurement.model.event.AddTraceTaskEvent;
import edu.usc.enl.dynamicmeasurement.model.event.Event;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 10/7/13
 * Time: 9:17 PM  <br/>
 * To generate a configuration file for a set of traffic transforms
 */
public class ScenarioManyTransformGenerator {
    protected final Document doc;
    protected final Element rootNode;
    private final Element addTransformPrototypeElement;
    private final Element removeTransformPrototypeElement;
    private int transformNum = 0;

    public ScenarioManyTransformGenerator(Element addTransformPrototypeElement, Element otherElementsRoot, Element removeTransformPrototypeElement) throws ParserConfigurationException {
        this.addTransformPrototypeElement = addTransformPrototypeElement;
        this.removeTransformPrototypeElement = removeTransformPrototypeElement;
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        doc = docBuilder.newDocument();
        rootNode = (Element) doc.importNode(otherElementsRoot, true);
        doc.appendChild(rootNode);
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        WildcardPattern.TOTAL_LENGTH = 32;
        String addTransformPrototypeFile = "resource/scenario/addSkewTransformTemplate.xml";
        String removeTransformPrototypeFile = "resource/scenario/removeTransformTemplate.xml";
        String otherElementsPrototypeFile = "resource/scenario/emptyEventTemplate.xml";
        String taskInputFile = "output\\sketch_sosr\\skew\\hh2\\hh_acc_real1/events.xml";
        new ScenarioManyTransformGenerator(ConfigReader.readElement(addTransformPrototypeFile),
                ConfigReader.readElement(otherElementsPrototypeFile), ConfigReader.readElement(removeTransformPrototypeFile)).run(taskInputFile);
    }

    //May produce fewer transforms comparing to events.xml because it may have repetitive task filters
    private void run(String input) {
        //for each task wildcard pattern

        Map<WildcardPattern, Integer> output = new HashMap<>();
        try {
            Element rootElement2 = ConfigReader.loadFile(new File(input));
            LinkedList<Event> events = new LinkedList<>();
            ConfigReader.loadEvents(events, rootElement2);
            for (Event event : events) {
                if (event instanceof AddTraceTaskEvent || event instanceof AddTaskEvent) {
                    makeTaskTransforms(event);

                }
            }
            commit(new File(input).getAbsoluteFile().getParent() + "/transform.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addTransform(WildcardPattern filter, String id, int time, int length) {
        Element taskElement = Util.getChildrenProperties2(addTransformPrototypeElement, "Property").get(0);
        taskElement.setAttribute(ConfigReader.PROPERTY_NAME, id);
        //set task filter
        NodeList descendantPropertyNodes = taskElement.getElementsByTagName("Property");
        for (int i = 0; i < descendantPropertyNodes.getLength(); i++) {
            Element item = (Element) descendantPropertyNodes.item(i);
            if (item.getAttribute(ConfigReader.PROPERTY_NAME).equals("Filter")) {
                item.setAttribute("value", filter.toStringNoWeight());
            }
        }
        //set event time
        addTransformPrototypeElement.setAttribute("time", time + "");
        setEventParams(addTransformPrototypeElement);

        Element taskElement2 = Util.getChildrenProperties2(removeTransformPrototypeElement, "Property").get(0);
        taskElement2.setAttribute(ConfigReader.PROPERTY_VALUE, id);
        removeTransformPrototypeElement.setAttribute("time", (time + length) + "");


        Node node = doc.importNode(addTransformPrototypeElement, true);
        rootNode.appendChild(node);
        node = doc.importNode(removeTransformPrototypeElement, true);
        rootNode.appendChild(node);
    }

    private void setEventParams(Element addTransformPrototypeElement) {
        double skew;
//        if (transformNum % 2 == 0) {
//            skew = 0.8;
//        } else {
        skew = 0.4;
//        }

        NodeList descendantPropertyNodes = addTransformPrototypeElement.getElementsByTagName("Property");
        for (int i = 0; i < descendantPropertyNodes.getLength(); i++) {
            Element item = (Element) descendantPropertyNodes.item(i);
            if (item.getAttribute(ConfigReader.PROPERTY_NAME).equals("Skew")) {
                item.setAttribute("value", String.format("%.2f", skew));
            }
        }
    }

    private void commit(String fileName) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            File f = new File(fileName).getAbsoluteFile();
            f.getParentFile().mkdirs();
            StreamResult result = new StreamResult(f);
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private List<Event> makeTaskTransforms(Event event) {
        int epoch = event.getEpoch();
        List<Event> transforms = new ArrayList<>();
        NodeList descendantPropertyNodes = event.getElement().getElementsByTagName("Property");
        for (int i = 0; i < descendantPropertyNodes.getLength(); i++) {
            Element item = (Element) descendantPropertyNodes.item(i);
            if (item.getAttribute(ConfigReader.PROPERTY_NAME).equals("Filter")) {
                String pattern = item.getAttribute(ConfigReader.PROPERTY_VALUE);
                WildcardPattern wildcardPattern = new WildcardPattern(pattern, 0);
                getTransforms(wildcardPattern, epoch);
                break;
            }
        }
        return transforms;
    }

    private void getTransforms(WildcardPattern wildcardPattern, int start) {
        int time = start;
        int taskLength = 300;
        int num = 1;
        for (int i = 0; i < num; i++) {
            addTransform(wildcardPattern, "t_" + transformNum++, time, taskLength / num);
            time += taskLength / num;
        }
    }

}
