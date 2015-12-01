package edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.stableskew;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.entropy.EntropyAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 1/5/14
 * Time: 7:17 AM <br/>
 * The implementation of entropy calculation using a stable skew distribution in a projection algorithm.
 * See Li, Ping, and Cun-Hui Zhang. "A New Algorithm for Compressed Counting with Applications in Shannon Entropy Estimation
 * in Dynamic Data." In COLT, pp. 477-496. 2011.
 * <br/>
 * It uses a chache for the random numbers to not recompute them for every packet
 * <br/>
 * TODO: Needs to be fixed to respect allocation and estimate accuracy
 */
public class StableSkewJ extends EntropyAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    public static final long MOD = (1l << 31) - 1;
    public int k = 100;
    public double alpha = 0.9999;
    private double[] counters;
    private long[][] hasha;
    private long[][] hashb;
    private double sum = 0;
    private Map<Long, CacheEntry> cacheEntryMap = new HashMap<>();
    //    private Map<Long, Double> ipSizeMap = new HashMap<>();
//    private ControlledBufferWriter countersWriter;
    private int step = 0;

    public StableSkewJ(Element element) {
        super(element);
        Map<String, Element> childrenProperties = Util.getChildrenProperties(element, "Property");
        k = Integer.parseInt(childrenProperties.get("k").getAttribute(ConfigReader.PROPERTY_VALUE));
        alpha = Double.parseDouble(childrenProperties.get("alpha").getAttribute(ConfigReader.PROPERTY_VALUE));
        counters = new double[k];
        Random sr = new Random(9237942749274l);
        hasha = new long[k][];
        hashb = new long[k][];
        for (int i = 0; i < k; i++) {
            hasha[i] = new long[]{sr.nextLong(), sr.nextLong()};
            hashb[i] = new long[]{sr.nextLong(), sr.nextLong()};
        }
    }

//    @Override
//    public void setFolder(String folder) {
//        super.setFolder(folder);
//        try {
//            countersWriter = Util.getNewWriter(folder + "/counters.csv");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void match(long item, double diff) {
        CacheEntry cacheEntry = cacheEntryMap.get(item);
        if (cacheEntry == null) {
            cacheEntry = new CacheEntry();
            cacheEntryMap.put(item, cacheEntry);
        }
        for (int i = 0; i < k; i++) {
            counters[i] += cacheEntry.get(i, item) * diff;
//            if (Double.isNaN(counters[i])) {
//                System.out.println("step " + step + " counter " + i + " " + cacheEntry.get(i, item) + " " + item);
//                System.exit(1);
//            }
        }
        sum += diff;

//        Double aDouble = ipSizeMap.get(item);
//        if (aDouble == null) {
//            aDouble = 0d;
//        }
//        ipSizeMap.put(item, aDouble + diff);
    }

    private long hashU(long a, long b, long x) {
        long result = (a * x) + b;
        long l = result - (result / (MOD - 1)) * (MOD - 1);
        while (l < 0) {
            l += MOD - 1;
        }
        return l; //should not contain 0 or 1
    }

    /**
     * hashes the x based on hash numbers a and b. The output must be >0
     *
     * @param a
     * @param b
     * @param x
     * @return
     */
    private long hashE(long a, long b, long x) {
        long result = (a * x) + b;
        long l = result & MOD;
        if (l == 0) {
            l = 1;//TODO: This fix changes the probability of 1
        }
        return l;
    }

    /**
     * @param item
     * @param i
     * @return a stableskew random number specifically based on item and i
     */
    private double getRandom(long item, int i) {
//        double v = Math.PI * (hashU(hasha[i][0], hashb[i][0], item) + 1) / (MOD + 1);
        double v = Math.PI * hashE(hasha[i][0], hashb[i][0], item) / (MOD + 1);
        double w = 1.0 * hashE(hasha[i][1], hashb[i][1], item) / (MOD + 1);
        w = -Math.log(w);//exponential distribution \lambda=1
        double delta = 1 - alpha;
        return Math.sin(alpha * v) * Math.pow(Math.sin(v), -1 / alpha) *
                Math.pow(Math.sin(v * delta) / w, delta / alpha);
    }


    public double findEntropy() {
        if (sum == 0) {
            return 0;
        }
        double jHat = 0;
        double delta = 1 - alpha;
        double power = -Math.round(alpha / delta);
        for (double counter : counters) {
            jHat += Math.pow(counter / sum, power);
        }
        jHat = jHat * delta / counters.length;

        //note that I moved sum inside the definition of jHat because of computation limits.
        //Thus we need to multiply that again to that. However, it actually cancels out with the
        //power *log(sum) in the output.
//        double output = -(Math.log(jHat) + power * Math.log(sum)) + power * Math.log(sum);
        double output = -Math.log(jHat);
        if (Double.isNaN(output) || Double.isInfinite(output)) {
            System.err.println("Nan or infinite entropy output " + Arrays.toString(counters));
        }

        step++;
        return output;
    }

    @Override
    public void reset() {
        super.reset();
        for (Iterator<Long> iterator = cacheEntryMap.keySet().iterator(); iterator.hasNext(); ) {
            CacheEntry cacheEntry = cacheEntryMap.get(iterator.next());
            if (cacheEntry.clean()) {
                iterator.remove();
            }
        }
        Arrays.fill(counters, 0);
        sum = 0;

//        ipSizeMap.clear();
    }

    @Override
    public void setCapacityShare(int resource) {

    }

    @Override
    public double estimateAccuracy() {
        return 1;
    }

    @Override
    public int getUsedResourceShare() {
        return 0;
    }

//    @Override
//    public void finish(FinishPacket p) {
//        super.finish(p);
//        countersWriter.close();
//    }

    double[] getCounters() {
        return counters;
    }

    public double getSum() {
        return sum;
    }

    /**
     *
     */
    private class CacheEntry {
        private int notSeen = 0;
        private boolean seen = false;
        private double[] r;

        private CacheEntry() {
            r = new double[k];
            Arrays.fill(r, -1);
        }

        public void set(int i, double randomItem) {
            r[i] = randomItem;
        }

        /**
         * @param i
         * @param item
         * @return the random number associated with ith counter for this specific item
         */
        public double get(int i, long item) {
            seen = true;
            if (r[i] < 0) {
                r[i] = getRandom(item, i);
            }
            return r[i];
        }

        /**
         * @return true if clean is called 3 times without this cacheentry has been hit
         */
        public boolean clean() {
            if (!seen) {
                notSeen++;
            } else {
                notSeen = 0;
            }
            seen = false;
            return notSeen > 3;
        }
    }
}
