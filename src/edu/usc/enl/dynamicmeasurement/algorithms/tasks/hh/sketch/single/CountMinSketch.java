package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.sketch.single;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hh.HHAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.AbstractHierarchicalCountMinSketch;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.PIRandomGenerator;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.ControlledBufferWriter;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 8/16/13
 * Time: 8:55 AM <br/>
 * Finds heavy hitters using a set and one count-min sketch.
 * <p>The implementationis from Cormode, Graham, and Marios Hadjieleftheriou.
 * "Finding frequent items in data streams." Proceedings of the VLDB Endowment 1.2 (2008): 1530-1541.</p>
 * <p>At arrival of each item, it updates the sketch and estimates the size of the item.
 * If its size>threshold it will be added to the set.
 * It reports all the items in the set and re-estimates their sizes.
 * </p>
 * TODO: must respect the task filter in creating output items and match
 */
public class CountMinSketch extends HHAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    private static int[] counts2 = new int[256];
    private Semaphore semaphore = new Semaphore(1);
    /**
     * Number of hash functions
     */
    private final int depth;
    /**
     * Sketch array
     */
    private final int[] counts;
    /**
     * Sketch hash functions parameters
     */
    private final int[] hasha, hashb;
    /**
     * The set of items that once at their arrival we thought they are heavy hitters.
     */
    private final Set<WildcardPattern> hhs = new HashSet<>();
    protected ControlledBufferWriter accuracyWriter;
    /**
     * The capacity allocated to this sketch
     */
    private int capWidth;
    private double sum;
    private double lastAccuracy;

    public CountMinSketch(Element element) {
        super(element);
        int depth = AbstractHierarchicalCountMinSketch.FIXED_DEPTH;
        int width = Util.getNetwork().getFirstMonitorPoints().getCapacity() / depth;
        this.depth = depth;
        counts =
                counts2;
//                new int[width * depth];
        capWidth = width;
        hasha = new int[depth];
        hashb = new int[depth];
        init();
    }


    public CountMinSketch(double threshold, int width, int depth, int wildcardNum1, WildcardPattern taskWildcardPattern1) {
        super(threshold, wildcardNum1, taskWildcardPattern1);
        this.depth = depth;
        counts = new int[width * depth];
        capWidth = width;
        hasha = new int[depth];
        hashb = new int[depth];
        init();
    }

    @Override
    public void setFolder(String folder) {
        super.setFolder(folder);
        try {
            accuracyWriter = Util.getNewWriter(folder + "/acc.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds the precision of heavy hitters provided as the keys of hhCollision.
     * This method uses the false probability scaling approach:
     * If most of found HHs are close to the threshold, precision should be smaller or
     * if there are many HHs far from the threshold, those few ones close to the threshold should also be OK
     *
     * @param hhCollision the map of heavy hitters to the used index (min index)
     * @param hhSum       summation of HHs that we are sure they are real hhs
     * @param collisions
     * @param num
     * @param sureHHs     will be filled with hhs that we are sure of them
     * @return precision in [0,1]
     */
    public double findPrecision(Map<WildcardPattern, Integer> hhCollision, double hhSum, double collisions, int num, List<WildcardPattern> sureHHs) {
        if (hhCollision.size() == 0) {
            return 1;
        }
        double output = 0;
        double newSum = sum - hhSum;
        if (newSum < 0) {
            newSum = sum;
        }

        // find the number of HHs that are close to the threshold
        double count = 0;
        for (Map.Entry<WildcardPattern, Integer> entry : hhCollision.entrySet()) {
            double excess = (entry.getKey().getWeight() - threshold) / newSum;
            double falseProbability = Math.min(1, Math.pow(1.0 / (capWidth * excess), depth));
            if (falseProbability >= 1) {
                count++;
            }
        }

        // the scale of the false probability
        double scale = count / num;

        // now estimate the precision
        for (Map.Entry<WildcardPattern, Integer> entry : hhCollision.entrySet()) {
            double excess = (entry.getKey().getWeight() - threshold) / newSum;
            double falseProbability = Math.min(1, Math.pow(1.0 / (capWidth * excess), depth));
            double p = (1 - falseProbability * scale); //* Math.pow(collisions / num - 1, 2);
            p /= entry.getValue();
            output += p;// * Math.exp(-1.0 * collisions / num);
            if (p > 0.9) {
                sureHHs.add(entry.getKey());
            }
//            System.out.println(getStep() + "," + entry.getKey() + "," + entry.getValue() + "," + p);
        }
//        System.out.println(1.0 * count / num);
//        output *= Math.exp(-1.0 * collisions / num);
        return output / num;
    }

    private void init() {
        //        prng = new PIRandomGenerator(-12784, 2);
        SecureRandom sr = new SecureRandom();
//        Random sr = new Random(234212322234l);
//        System.out.println(sr.getProvider());
        // initialize the generator for picking the hash functions


        for (int k = 0; k < depth; k++) { // pick the hash functions
            hasha[k] = (int) (sr.nextInt() & PIRandomGenerator.MOD);
            hashb[k] = (int) (sr.nextInt() & PIRandomGenerator.MOD);
        }
    }

//    public void setCountersFile(String file) {
//        try {
//            pw = new PrintWriter(file);
//            accuracyPw = new PrintWriter(new File(file).getParentFile().getPath() + "/acc.txt");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public Collection<WildcardPattern> findHH() {

        //find collided items
        IntegerWrapper index = new IntegerWrapper(0);
        Map<Integer, Integer> usedCounters = new HashMap<>();
        Map<WildcardPattern, Integer> hhhCounter = new HashMap<>();
        for (WildcardPattern wildcardPattern : hhs) {
            wildcardPattern.setWeight(CMH_count(wildcardPattern.getData(), index));
            hhhCounter.put(wildcardPattern, index.getValue());
            if (!usedCounters.containsKey(index.getValue())) {
                usedCounters.put(index.getValue(), 1);
            } else {
                usedCounters.put(index.getValue(), usedCounters.get(index.getValue()) + 1);
            }
           /* long[] longs = CMH_all(wildcardPattern.getData());
            pw.print(step + "," + wildcardPattern.toStringNoWeight());
            for (long aLong : longs) {
                pw.print("," + aLong);
            }
            pw.println();*/
        }
        //collision: items that have the same minimum counter
        int collisions = 0;
        for (Integer integer : usedCounters.values()) {
            if (integer > 1) {
                collisions += integer - 1;
            }
        }

        //find sum of not colliding items
        long hhSum = 0;
        Map<WildcardPattern, Integer> hhhCollision = new HashMap<>();
        for (Map.Entry<WildcardPattern, Integer> entry : hhhCounter.entrySet()) {
            Integer collisions1 = usedCounters.get(entry.getValue());
            hhhCollision.put(entry.getKey(), collisions1);
            if (collisions1 == 1) {
                hhSum += Math.max(0, entry.getKey().getWeight() - sum / (capWidth));
            }
        }
        lastAccuracy = findPrecision(hhhCollision, hhSum, collisions, hhs.size(), new ArrayList<WildcardPattern>());
        accuracyWriter.println(getStep() + "," + lastAccuracy + "," + collisions);
        return hhs;
    }

    private List<WildcardPattern> findCollisions(Map<WildcardPattern, Integer> hhCounter) {
        BitSet counters = new BitSet(hhCounter.size());
        List<WildcardPattern> output = new LinkedList<>();
        for (Map.Entry<WildcardPattern, Integer> entry : hhCounter.entrySet()) {
            if (counters.get(entry.getValue())) {
                output.add(entry.getKey());
            }
            counters.set(entry.getValue());
        }
        return output;
    }

    private long[] CMH_all(long item) {
        long[] output = new long[depth];
        int j;
        int offset;

        offset = 0;//because all for a level for all hash functions are in 1D array
        for (j = 0; j < this.depth; j++) {
            output[j] = counts[(PIRandomGenerator.hash31(hasha[j], hashb[j], item) % capWidth) + offset];
            offset += capWidth;
        }
        return output;
    }

    /**
     * Estimate the size of an item using the sketch
     *
     * @param item
     * @param index will contain the min index
     * @return
     */
    private long CMH_count(long item, IntegerWrapper index) {
        // return an estimate of item at level depth

        int j;
        int offset;
        long estimate;

        offset = 0;//because all for a level for all hash functions are in 1D array
        int index2 = (PIRandomGenerator.hash31(hasha[0], hashb[0], item) % capWidth) + offset;
        estimate = counts[index2];
        if (index != null) {
            index.setValue(index2);
        }
        for (j = 1; j < this.depth; j++) {
            offset += capWidth;
            index2 = (PIRandomGenerator.hash31(hasha[j], hashb[j], item) % capWidth) + offset;
            long v = counts[index2];
            if (v < estimate) {
                estimate = v;
                if (index != null) {
                    index.setValue(index2);
                }
            }
        }
        return (estimate);
    }

    @Override
    public void reset() {
        Arrays.fill(counts, 0);
        hhs.clear();
    }

    @Override
    public void finish() {
        accuracyWriter.close();
    }

    @Override
    public void setSum(double sum) {
        this.sum = sum;
    }

    public void match(long item, double diff) {
        try {
            semaphore.acquire();
            item >>>= wildcardNum;
            int i, j, offset;

            offset = 0;
            for (j = 0; j < depth; j++) {
                counts[(PIRandomGenerator.hash31(hasha[j], hashb[j], item)
                        % capWidth) + offset] += diff;
                // this can be done more efficiently if the width is a power of two
                offset += capWidth;
            }

            if (CMH_count(item, null) > threshold) {
                hhs.add(new WildcardPattern(item, wildcardNum, 0));
            }
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setCapacity(int c) {
//        capWidth = c;
        capWidth = 256 / depth;
    }

    @Override
    public double estimateAccuracy() {
        return lastAccuracy;
    }

    @Override
    public int getUsedResourceShare() {
        return capWidth;
    }

    @Override
    public void setCapacityShare(int c) {
        setCapacity(c / depth);
    }
}
