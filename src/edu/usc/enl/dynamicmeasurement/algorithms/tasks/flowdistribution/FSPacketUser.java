package edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.TaskUser;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.data.DataPacket;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.util.ControlledBufferWriter;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/6/2014
 * Time: 1:07 PM
 */
public class FSPacketUser extends TaskUser {
    private final FSAlgorithm fsAlgorithm;
    private ControlledBufferWriter reportPrintWriter;

    public FSPacketUser(Element element) throws Exception {
        super(element);
        Element algorithmElement = Util.getChildrenProperties(element, "Property").get("Algorithm");
        this.fsAlgorithm = (FSAlgorithm) Class.forName(algorithmElement.getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class).newInstance(algorithmElement);
    }

    @Override
    public void setFolder(String folder) {
        try {
            if (reportPrintWriter != null) {
                reportPrintWriter.close();
            }
            reportPrintWriter = Util.getNewWriter(folder + "/hhh.csv");
            fsAlgorithm.setFolder(folder);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void reportFS(Map<Long, Long> fs, int step) {
//        System.out.println(report + ": " + hhh.size() + " hhhs");
        List<Long> items = new ArrayList<>(fs.keySet());
        Collections.sort(items);
        for (Long item : items) {
            reportPrintWriter.println(step + "," + item + "," + fs.get(item));
        }
        reportPrintWriter.flush();
    }

    @Override
    public Task2.TaskImplementation getImplementation() {
        return fsAlgorithm;
    }

    @Override
    public void report(int step) {
        fsAlgorithm.setStep(step);
        Map<Long, Long> fs = fsAlgorithm.findFS();
        reportFS(fs, step);
    }

    @Override
    public void update(int step) {
        fsAlgorithm.setStep(step);
        fsAlgorithm.doUpdate();
        reset();
    }

    private void reset() {
        fsAlgorithm.reset();
    }

    @Override
    public void process2(DataPacket p) {
        fsAlgorithm.match(p.getSrcIP(), 1);
    }

    @Override
    public void finish(FinishPacket p) {
        fsAlgorithm.finish();
        reportPrintWriter.close();
    }
}
