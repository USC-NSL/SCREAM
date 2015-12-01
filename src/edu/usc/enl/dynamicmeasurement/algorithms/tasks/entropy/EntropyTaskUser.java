package edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.Task2;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.TaskUser;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.data.DataPacket;
import edu.usc.enl.dynamicmeasurement.data.FinishPacket;
import edu.usc.enl.dynamicmeasurement.util.ControlledBufferWriter;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 1/4/14
 * Time: 8:45 PM  <br/>
 * This class implements change detection task and uses a EntropyAlgorithm.
 * <p>The XML constructor requires a Property tag with name attribute as "Algorithm"
 * and with value attribute pointing to a class that implements EntropyAlgorithm.</p>
 *
 * @see edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.EntropyAlgorithm
 */
public class EntropyTaskUser extends TaskUser {
    private EntropyAlgorithm algorithm;
    private ControlledBufferWriter reportPrintWriter;

    public EntropyTaskUser(Element element) throws Exception {
        super(element);
        Element algorithmElement = Util.getChildrenProperties(element, "Property").get("Algorithm");
        this.algorithm = (EntropyAlgorithm) Class.forName(algorithmElement.getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class).newInstance(algorithmElement);
    }

    @Override
    public void setFolder(String folder) {
        algorithm.setFolder(folder);
        try {
            if (reportPrintWriter != null) {
                reportPrintWriter.close();
            }
            reportPrintWriter = Util.getNewWriter(folder + "/entropy.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Task2.TaskImplementation getImplementation() {
        return (Task2.TaskImplementation) algorithm;
    }

    @Override
    public void report(int step) {
        double entropy = algorithm.findEntropy();
        reportPrintWriter.println(step + "," + entropy);
    }

    @Override
    public void update(int step) {
        algorithm.reset();
    }

    @Override
    public void process2(DataPacket p) {
        algorithm.match(p.getSrcIP(), p.getSize());
    }

    @Override
    public void finish(FinishPacket p) {
        algorithm.finish(p);
        reportPrintWriter.close();
    }
}
