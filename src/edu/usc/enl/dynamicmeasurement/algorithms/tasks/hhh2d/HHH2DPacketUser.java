package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh2d;

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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/2/2014
 * Time: 4:57 AM
 */
public class HHH2DPacketUser extends TaskUser {
    private final HHH2DAlgorithm hhh2dAlgorithm;
    private ControlledBufferWriter reportPrintWriter;

    public HHH2DPacketUser(Element element) throws Exception {
        super(element);
        Element algorithmElement = Util.getChildrenProperties(element, "Property").get("Algorithm");
        this.hhh2dAlgorithm = (HHH2DAlgorithm) Class.forName(algorithmElement.getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class).newInstance(algorithmElement);
    }

    @Override
    public void setFolder(String folder) {
        try {
            if (reportPrintWriter != null) {
                reportPrintWriter.close();
            }
            reportPrintWriter = Util.getNewWriter(folder + "/hhh.csv");
            hhh2dAlgorithm.setFolder(folder);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void reportHHH2D(Collection<WildcardPatternND> hhh, int step) {
        List<WildcardPatternND> hhh_Sorted = new ArrayList<>(hhh);
        Collections.sort(hhh_Sorted);
        for (WildcardPatternND wildcardPattern : hhh) {
            reportPrintWriter.println(step + "," + wildcardPattern.toStringNoWeight() + "," + wildcardPattern.getWeight());
        }
        reportPrintWriter.flush();
    }

    @Override
    public Task2.TaskImplementation getImplementation() {
        return hhh2dAlgorithm;
    }

    @Override
    public void report(int step) {
        hhh2dAlgorithm.setStep(step);
        Collection<WildcardPatternND> hhh = hhh2dAlgorithm.findHHH();
        reportHHH2D(hhh, step);
    }

    @Override
    public void update(int step) {
        hhh2dAlgorithm.setStep(step);
        hhh2dAlgorithm.doUpdate();
        reset();
    }

    private void reset() {
        hhh2dAlgorithm.reset();
    }

    @Override
    public void process2(DataPacket p) {
        hhh2dAlgorithm.match(p.getSrcIP(), p.getDstIP(), p.getSize());
    }

    @Override
    public void finish(FinishPacket p) {
        hhh2dAlgorithm.finish();
        reportPrintWriter.close();
    }
}
