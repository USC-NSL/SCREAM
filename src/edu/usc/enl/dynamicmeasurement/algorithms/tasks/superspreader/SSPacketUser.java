package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.TaskUser;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.data.DataPacket;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.ControlledBufferWriter;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 1:07 PM
 */
public class SSPacketUser extends TaskUser {
    private final SSAlgorithm ssAlgorithm;
    private ControlledBufferWriter reportPrintWriter;

    public SSPacketUser(Element element) throws Exception {
        super(element);
        Element algorithmElement = Util.getChildrenProperties(element, "Property").get("Algorithm");
        this.ssAlgorithm = (SSAlgorithm) Class.forName(algorithmElement.getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class).newInstance(algorithmElement);
    }

    @Override
    public void setFolder(String folder) {
        try {
            if (reportPrintWriter != null) {
                reportPrintWriter.close();
            }
            reportPrintWriter = Util.getNewWriter(folder + "/hhh.csv");
            ssAlgorithm.setFolder(folder);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void reportSS(Collection<WildcardPattern> hhh, int step) {
//        System.out.println(report + ": " + hhh.size() + " hhhs");
        List<WildcardPattern> hhh_Sorted = new ArrayList<>(hhh);
        Collections.sort(hhh_Sorted);
        for (WildcardPattern wildcardPattern : hhh) {
            reportPrintWriter.println(step + "," + wildcardPattern.toStringNoWeight() + "," + wildcardPattern.getWeight());
        }
        reportPrintWriter.flush();
    }

    @Override
    public Task2.TaskImplementation getImplementation() {
        return ssAlgorithm;
    }

    @Override
    public void report(int step) {
        ssAlgorithm.setStep(step);
        Collection<WildcardPattern> hhh = ssAlgorithm.findSS();
        reportSS(hhh, step);
    }

    @Override
    public void update(int step) {
        ssAlgorithm.setStep(step);
        ssAlgorithm.doUpdate();
        reset();
    }

    private void reset() {
        ssAlgorithm.reset();
    }

    @Override
    public void process2(DataPacket p) {
        try {
            ssAlgorithm.match((long) getKeyField.invoke(p), p.getSrcIP());
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish(FinishPacket p) {
        ssAlgorithm.finish();
        reportPrintWriter.close();
    }
}
