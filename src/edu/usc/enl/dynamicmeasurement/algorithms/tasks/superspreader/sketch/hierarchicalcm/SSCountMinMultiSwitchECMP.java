package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.util.MonitorPointData;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/10/2014
 * Time: 11:01 AM
 */
public class SSCountMinMultiSwitchECMP extends SSCountMinMultiSwitch2 {
    private List<MonitorPointData> switches = new ArrayList<>();
    private HashFunction hashFunction = Hashing.murmur3_32(923749928);

    public SSCountMinMultiSwitchECMP(Element element) {
        super(element);
    }

    @Override
    public void match(long key, long item) {
        switches.clear();
        sketchMultiSwitch.getMonitorPoints().stream().filter(monitorPoint -> monitorPoint.getMonitorPoint().hasDataFrom(key)).forEach(switches::add);
        int index = Math.abs(hashFunction.hashLong(item).asInt()) % switches.size(); //may not be fair
        switches.get(index).getSketch().match(key, item);
    }
}
