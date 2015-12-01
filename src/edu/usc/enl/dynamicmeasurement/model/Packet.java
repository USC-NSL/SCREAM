package edu.usc.enl.dynamicmeasurement.model;

import edu.usc.enl.dynamicmeasurement.algorithms.transform.TrafficTransformer;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 8/4/13
 * Time: 5:07 PM <br/>
 * A packet in the simulator. This is not necessarily a packet from trace, but it can be used for passing
 * essential information among simulator components.
 */
public class Packet {
    protected long time;
    protected TrafficTransformer lastTransformer;

    public Packet(long time) {
        this.time = time;
    }

    protected Packet() {

    }

    public long getTime() {
        return time;
    }

    public TrafficTransformer getLastTransformer() {
        return lastTransformer;
    }

    public void setLastTransformer(TrafficTransformer lastTransformer) {
        this.lastTransformer = lastTransformer;
    }
}
