package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.HHHAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.LongWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/7/2014
 * Time: 11:10 AM
 */
public abstract class AbstractHierarchicalCountMinSketch extends HHHAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    public static final int FIXED_DEPTH = 3;
    protected final boolean useChildSum = false;
    public int[] indexArray;
    protected long sum;
    protected int levels; // function of U and gran
    protected int U; // size of the universe in bits
    protected int gran; // granularity: eg 1, 4 or 8 bits
    protected int depth;
    protected double lastAccuracy;
    protected PrintWriter accuracyWriter;
    protected boolean zeroWidth = false;
    protected PrecisionEstimationData precisionEstimationData;
    private int freeLim; // up to which level to keep exact counts
    private int width;
    protected int capWidth;
    protected int[][] hasha, hashb;
    protected IntegerWrapper indexWrapper = new IntegerWrapper(0);
    private int capacity = 0;
    private boolean cannotTraverseAll;

    public AbstractHierarchicalCountMinSketch(double threshold, WildcardPattern taskWildcardPattern1, int depth, int U,
                                              int gran, int[][] hashaa, int[][] hashbb, int capacity, int aCounterSize) {
        super(threshold, taskWildcardPattern1);
        this.levels = (int) Math.ceil(1.0 * (getTaskWildcardPattern().getWildcardNum() - (edu.usc.enl.dynamicmeasurement.model.WildcardPattern.TOTAL_LENGTH - U)) / gran);
        capacity /= aCounterSize;
        int exactLevels = getExactCounterForCapacity(levels, capacity);
        int width = (capacity - (1 << (exactLevels + 1))) / depth / (levels - exactLevels);
        if (width == 0) {
            throw new RuntimeException("0 width");
        }
        init(width, depth, U, gran, hashaa, hashbb);
    }

    public AbstractHierarchicalCountMinSketch(double threshold, WildcardPattern taskWildcardPattern1, int width, int depth, int U,
                                              int gran, int[][] hashaa, int[][] hashbb) {
        super(threshold, taskWildcardPattern1);
        init(width, depth, U, gran, hashaa, hashbb);
    }

    public AbstractHierarchicalCountMinSketch(Element element) {
        super(element);
//        Map<String, Element> properties = Util.getChildrenProperties(element, "Property");
//        int width = Integer.parseInt(properties.get("Width").getAttribute(ConfigReader.PROPERTY_VALUE));
//        int depth = Integer.parseInt(properties.get("Depth").getAttribute(ConfigReader.PROPERTY_VALUE));
//        int l = Integer.parseInt(properties.get("L").getAttribute(ConfigReader.PROPERTY_VALUE));

        int depth = FIXED_DEPTH;
        int l = 0;
        int gran = 1;
        U = computeU(l);
        this.levels = (int) Math.ceil(1.0 * (getTaskWildcardPattern().getWildcardNum() - (edu.usc.enl.dynamicmeasurement.model.WildcardPattern.TOTAL_LENGTH - U)) / gran);
        int capacity1 = Util.getNetwork().getFirstMonitorPoints().getCapacity();
        capacity1 /= getACounterSize(element);
        int exactLevels = getExactCounterForCapacity(levels, capacity1);
        int width = (capacity1 - (1 << (exactLevels + 1))) / depth / (levels - exactLevels);
        if (width == 0) {
            throw new RuntimeException("0 width");
        }
        init(width, depth, U, gran, null, null);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getTaskWildcardPattern().toStringNoWeight() + ")";
    }

    public static int getExactCounterForCapacity(int levels, int capacity) {
        int exactLevels = 0;
        for (int i = 1; i < levels; i++) {
            long usedCapacity = (levels - i + 2) * (1l << i); //note levels starts from 0.
            //2 is for ith level and all levels above it
            if (usedCapacity > capacity) {
                exactLevels = i - 1;
                break;
            }
        }
        return exactLevels;
    }

    protected abstract int getACounterSize(Element element);

    /**
     * add counters from subsketches to this
     *
     * @param subSketches
     */
    public abstract void fillCounters(Collection<? extends AbstractHierarchicalCountMinSketch> subSketches);

    public void prepareToFindHHH() {
        cannotTraverseAll = false;
    }

    private void init(int width, int depth, int U, int gran, int[][] hashaa, int[][] hashbb) {
        precisionEstimationData =
                MakePrecisionEstimationData();

        if (U <= 0 || U > WildcardPattern.TOTAL_LENGTH) {
            throw new ExceptionInInitializerError("Invalid U");
        }
        // U is the log the size of the universe in bits

        if (gran > U || gran < 1) {
            throw new ExceptionInInitializerError("Invalid gran");
        }
        // gran is the granularity to look at the universe in
        // check that the parameters make sense...

//        prng = new PIRandomGenerator(-12784, 2);
        // initialize the generator for picking the hash functions

        this.depth = depth;
        this.width = width;
        capWidth = width;
        this.sum = 0;
        this.U = U;
        this.gran = gran;
        this.hasha = hashaa;
        this.hashb = hashbb;
        this.levels = (int) Math.ceil(1.0 * (getTaskWildcardPattern().getWildcardNum() - (WildcardPattern.TOTAL_LENGTH - U)) / gran);
        for (int j = 0; j < levels; j++) {
            if ((long) 1 << (gran * j) <= depth * width) {
                freeLim = j;
            } else {
                break;
            }
        }
        //find the level up to which it is cheaper to keep exact counts because we have enough counters
        freeLim = levels - freeLim;
        indexArray = new int[depth];

        if (hashaa == null) {
            initHashFunctions();
        }
        //initCounters();
    }

    protected PrecisionEstimationData MakePrecisionEstimationData() {
        return new PrecisionEstimationData(this);
//        return new StrawManPrecisionEstimationData(this);
    }

    protected abstract void createCounters(int levels);

    public boolean isZeroWidth() {
        return zeroWidth;
    }

    private void initHashFunctions() {
        Random sr = new Random(234212322234l);
        hasha = new int[levels][];
        hashb = new int[levels][];
        for (int i = 0; i < levels; i++) {
            hasha[i] = new int[depth];
            hashb[i] = new int[depth];


            for (int k = 0; k < depth; k++) { // pick the hash functions
                hasha[i][k] = (int) (sr.nextInt() & PIRandomGenerator.MOD);
                hashb[i][k] = (int) (sr.nextInt() & PIRandomGenerator.MOD);
            }
        }
    }

    //MUST BE CALLED IN CHILDREN
    protected void initCounters() {
        createCounters(levels);
        for (int i = 0; i < levels; i++) {
//            if (isExactCounter(i)) { // allocate space for representing things exactly at high levels
//                counts[i] = new int[1 << (gran * j)];//this can be smaller than depth*width
//                hasha[i] = null;
//                hashb[i] = null;
//            } else { // allocate space for a sketch
            createCounterAt(width, depth, i);
//            }
        }
    }

    protected abstract void createCounterAt(int width, int depth, int i);

    protected int computeU(int l) {
        return l == 0 ? 32 : ((int) (Math.log(depth * width) / Math.log(2) + l));
    }

    public PrecisionEstimationData getPrecisionEstimationData() {
        return precisionEstimationData;
    }

    public int[][] getHasha() {
        return hasha;
    }

    public int[][] getHashb() {
        return hashb;
    }

    public int getU() {
        return U;
    }

    public int getGran() {
        return gran;
    }

    public int getDepth() {
        return depth;
    }

    public int getSize() {
        return levels * depth * capWidth;
    }

    @Override
    public void reset() {
        precisionEstimationData.reset();
        //reset all sketches
//        for (int[] count : counts) {
//            if (count != null) {
//                Arrays.fill(count, 0);
//            }
//        }
        sum = 0;
    }

    @Override
    public Collection<WildcardPattern> findHHH() {

        List<WildcardPattern> output;
        computeSum();
        if (zeroWidth) {
            if (sum >= threshold) {
                lastAccuracy = 0;
            } else {
                lastAccuracy = 1;
            }
            output = new ArrayList<>();
        } else {
            HHHSet result = new HHHSet();
            CMH_recursive(levels, 0, result, new LongWrapper(0), precisionEstimationData.getHhTree(), threshold);
//            filterResult(result);
            output = fillOutput(result.getResult());
            precisionEstimationData.updateAccuracy(result.getResult(), output.size());
        }
//        System.out.println(getStep() + "," + lastAccuracy);
        accuracyWriter.println(getStep() + "," + lastAccuracy);

        return output;
    }

    public boolean filterLevel(int level) {
        return true;
    }

    protected abstract void computeSum();

    public long realToCounterInput(long item) {
        int ignoredBits = WildcardPattern.TOTAL_LENGTH - U;
        item -= getTaskWildcardPattern().getData() << getTaskWildcardPattern().getWildcardNum();
        item >>>= ignoredBits;
        return item;
    }

    public int getLevel(int wildcardNum) {
        int out = wildcardNum - (WildcardPattern.TOTAL_LENGTH - U);
        if (out < 0) {
            System.err.println("Negative level");
        }
        return out;
    }

    public abstract double getFalseProbability(double newSum, HHHOutput hhhOutput, int level, int[] badCounters);

    public static List<WildcardPattern> fillOutput(Map<Integer, List<HHHOutput>> result) {
        List<WildcardPattern> output = new LinkedList<>();
        for (List<HHHOutput> hhhs : result.values()) {
            for (HHHOutput hhh : hhhs) {
                output.add(hhh.getWildcardPattern());
            }
        }
        return output;
    }

    @Override
    public void finish() {
        super.finish();
        if (accuracyWriter != null) {
            accuracyWriter.close();
        }
    }

    /**
     * call this method for fringe nodes, for internal ones the CMH_recursive should do this itself
     *
     * @param minDepth
     * @param level
     * @return
     */
    protected long getChildSum(int minDepth, int level, long start) {
        // if level<maxDepth, call this method for each child
        // for each child get children sum and if their sum<child weight use their sum
        if (level > minDepth) {
            long sum = 0;
            int blockSize = 1 << gran;
            long itemShift = start << gran;
            // assumes that gran is an exact multiple of the bit dept
            for (int i = 0; i < blockSize; i++) {
                sum += getChildSum(minDepth, level - 1, itemShift + i);
            }
            long prediction = CMH_count(level, start);
//            if (sum < prediction) {
//                System.out.println("sum is smaller than estimation");
//            }
            return Math.min(prediction, sum);
        } else {
            return CMH_count(level, start);
        }
    }

    /**
     * this method is to gather the level hhs in the hhtree and not for creating the output
     *
     * @param level
     * @param start
     * @param childSum
     * @param hhTree
     * @param threshold
     */
//    protected boolean CMH_recursive(int level, long start, LongWrapper childSum, Map<Integer, List<LevelHH>> hhTree, double threshold) {
//        // for finding heavy hitters, recursively descend looking
//        // for ranges that exceed the threshold
//
//        int i;
//        int blockSize;
//        long estcount;
//        long itemShift;
//        boolean gatherHH = filterLevel(level);
//        boolean stop = false;
//
//        estcount = CMH_count(level, start);
//        if (estcount >= threshold) {
//            int levelHHsSize = 0;
//            if (gatherHH) {
//                List<LevelHH> levelHHs = hhTree.get(level);
//                if (levelHHs == null) {
//                    levelHHs = new LinkedList<>();
//                    hhTree.put(level, levelHHs);
//                }
//                LevelHH e = new LevelHH(estcount, indexWrapper.getValue(), indexArray);
//                levelHHs.add(e);
//                levelHHsSize = levelHHs.size();
//            }
//            if (level > 0) {
////                List<WildcardPattern> resTemp = new LinkedList<>();
//                blockSize = 1 << gran;
//                itemShift = start << gran;
//                // assumes that gran is an exact multiple of the bit dept
//                long sum = 0;
//                for (i = 0; i < blockSize && !stop; i++) {
//                    if (levelHHsSize > 2 * getSum() / threshold) {
//                        stop = true;
//                        break;
//                    }
//                    stop = CMH_recursive(level - 1, itemShift + i, childSum, hhTree, threshold);
//                    sum += childSum.getValue();
//                }
//                if (useChildSum) {
//                    estcount = Math.min(estcount, sum);
//                }
//            } else {
//                stop = (levelHHsSize > 2 * getSum() / threshold);
//            }
//            childSum.setValue(estcount);
//        } else if (useChildSum) {
//            if (estcount == 0) {
//                childSum.setValue(0);
//            } else {
//                childSum.setValue(Math.min(estcount, getChildSum(Math.max(0, level - 3), level, start)));
//            }
//        }
//        return stop;
//    }

    /**
     * This is for creating the output
     *
     * @param level
     * @param start
     * @param res
     * @param childSum
     * @param hhTree
     * @param threshold
     * @return the total weight of found HHHs
     */
    protected long CMH_recursive(int level, long start, HHHSet res, LongWrapper childSum,
                                 Map<Integer, List<LevelHH>> hhTree, double threshold) {
        // for finding heavy hitters, recursively descend looking
        // for ranges that exceed the threshold

        int i;
        int blockSize;
        long estcount;
        long itemShift;
        double maxHHH = Math.min(1000, 50 * Math.ceil(sum / threshold));
        estcount = CMH_count(level, start);
        boolean gatherHH = filterLevel(level);
        if (estcount >= threshold) {
            LevelHH levelHH = null;
            if (gatherHH) {
                List<LevelHH> levelHHs = hhTree.get(level);
                if (levelHHs == null) {
                    levelHHs = new LinkedList<>();
                    hhTree.put(level, levelHHs);
                }
                levelHH = new LevelHH(estcount, indexWrapper.getValue(), indexArray);
                levelHHs.add(levelHH);
            }
            long descendantHHHSum = 0;
            HHHSet childrenHHHs = new HHHSet();
            if (level > wildcardNum) {
//                List<WildcardPattern> resTemp = new LinkedList<>();
                blockSize = 1 << gran;
                itemShift = start << gran;
                // assumes that gran is an exact multiple of the bit dept
                long sum = 0;
                for (i = 0; i < blockSize; i++) {
                    if (res.size() + childrenHHHs.size() > maxHHH) {
                        break;
                    }
                    descendantHHHSum += CMH_recursive(level - 1, itemShift + i, childrenHHHs, childSum, hhTree, threshold);
                    sum += childSum.getValue();
                    if (res.size() + childrenHHHs.size() > maxHHH) {
                        cannotTraverseAll = true;
                        break;
                    }
                }
                if (useChildSum) {
                    estcount = Math.min(estcount, sum);
                }
                res.merge(childrenHHHs);
            }
            childSum.setValue(estcount);

            if (estcount - descendantHHHSum >= threshold && gatherHH) {
                if (res.size() < maxHHH) { // WHY? Haha now I understand because it can use all memory
                    int shifts = getRealWildcardNums(level);
                    WildcardPattern wildcardPattern = new WildcardPattern(getRealData(start, shifts),
                            shifts, estcount - descendantHHHSum);
                    HHHOutput hhh = getHhhOutput(wildcardPattern);
                    hhh.setAllCounters(levelHH.getAllCounters());
                    hhh.setMinIndex(levelHH.getCounterIndex());
                    res.add(hhh, level);
                    //if it is exact counter and there is no hhh children it is for sure an HHH
//                    if (isExactCounter(level) && childrenHHHs.size() == 0) {
//                        hhh.setPrecision(1);
//                    }
                    hhh.setDescendantHHHs(childrenHHHs);
                } else {
                }
                return estcount;
            }
            return descendantHHHSum;
        } else if (useChildSum) {
            if (estcount == 0) {
                childSum.setValue(0);
            } else {
                childSum.setValue(Math.min(estcount, getChildSum(Math.max(0, level - 3), level, start)));
            }
        }
        return 0;
    }

    public IntegerWrapper getIndexWrapper() {
        return indexWrapper;
    }

    protected HHHOutput getHhhOutput(WildcardPattern wildcardPattern) {
        return new HHHOutput(wildcardPattern);
    }

    protected int getRealWildcardNums(int level) {
        return gran * level + WildcardPattern.TOTAL_LENGTH - U;
    }

    protected long getRealData(long start, int shifts) {
        if (getTaskWildcardPattern().getWildcardNum() - shifts >= 0) {
            return start + (getTaskWildcardPattern().getData() <<
                    (getTaskWildcardPattern().getWildcardNum() - shifts));
        } else {
            return start;
        }
    }

    public long CMH_count(int level, long item) {
        return CMH_count(level, item, indexWrapper, indexArray);
    }

    public abstract long CMH_count(int level, long item, IntegerWrapper index, int[] indexArray);

    public boolean isExactCounter(int level) {
        return level >= freeLim;
    }

    @Override
    public double estimateAccuracy() {
        return lastAccuracy;
    }

    @Override
    public int getUsedResourceShare() {
        return capacity;
    }

    protected int getFreeLim() {
        return freeLim;
    }

    public int getCapWidth() {
        return capWidth;
    }

    public long getSum() {
        return sum;
    }

    @Override
    public void setFolder(String folder) {
        super.setFolder(folder);
        try {
            accuracyWriter = new PrintWriter(folder + "/acc.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCapacityShare(int c) {
        capacity = c;
        int exactLevels = getExactCounterForCapacity(levels, c);
        int width = (c - (1 << (exactLevels + 1))) / depth / (levels - exactLevels);
        //int width = (1 << exactLevels) / depth;
        if (width == 0) {
            zeroWidth = true;
        } else {
            zeroWidth = false;
        }
        capWidth = width;
        if (capWidth > this.getWidth2()) {
            System.err.println("Capwidth " + capWidth + " > width " + width + " in " + taskWildcardPattern);
        }
        //freeLim = levels - exactLevels;//don't use exact level as it has problem in corner case where depth is not pewer of two.
        //instread lets use whatever we used at init
        for (int j = 0; j < levels; j++) {
            if ((long) 1 << (gran * j) <= depth * width) {
                freeLim = j;
            } else {
                break;
            }
        }
        //find the level up to which it is cheaper to keep exact counts because we have enough counters
        freeLim = levels - freeLim;

    }

    public int getLevels() {
        return levels;
    }

    public int getWidth2() {
        return width;
    }

    public abstract int getCount(int level, int index);

    public void findBadCounters(Collection<HHHOutput> levelHHH, int[] badCountersBitmap, int level) {
        Arrays.fill(badCountersBitmap, 0);
        for (HHHOutput hhh : levelHHH) {
            double descendantSum = 0;
            for (Map.Entry<Integer, List<HHHOutput>> entry : hhh.getDescendantHHHs().getResult().entrySet()) {
                for (HHHOutput hhhOutput : entry.getValue()) {
                    descendantSum += hhhOutput.getWildcardPattern().getWeight();
                }
            }
            if (isExactCounter(level)) {
                badCountersBitmap[hhh.getMinIndex()] += hhh.getWildcardPattern().getWeight() + descendantSum;
            } else {
                for (int i : hhh.getAllCounters()) {
                    badCountersBitmap[i] += Math.min(getCount(level, i), hhh.getWildcardPattern().getWeight() + descendantSum);
                }
            }
        }
    }

    public void findBadCounters2(Collection<LevelHH> levelHHH, int[] badCountersBitmap, int level) {
        Arrays.fill(badCountersBitmap, 0);
        for (LevelHH hhh : levelHHH) {
            if (isExactCounter(level)) {
                badCountersBitmap[hhh.getCounterIndex()] += hhh.getWeight();
            } else {
                for (int i : hhh.getAllCounters()) {
                    badCountersBitmap[i] += Math.min(getCount(level, i), hhh.getWeight());
                }
            }
        }
    }

    public static class StrawManPrecisionEstimationData extends PrecisionEstimationData {

        public StrawManPrecisionEstimationData(AbstractHierarchicalCountMinSketch sketch) {
            super(sketch);
        }

        public void fixLevelHHSum() {
            for (int i = 0; i < sketch.getFreeLim(); i++) {
                levelHhsum.put(i, 0d);
            }
        }

        @Override
        public void updateAccuracy(Map<Integer, List<HHHOutput>> result, int outputSize) {
            if (outputSize == 0) {
                sketch.lastAccuracy = 1;
            } else {
                double precision = 0;
                int[] badCounters = new int[4];//TODO
                for (List<HHHOutput> hhhOutputs : result.values()) {
                    for (HHHOutput hhh : hhhOutputs) {
                        int level = sketch.getLevel(hhh.getWildcardPattern().getWildcardNum());
                        double falseProbability = 1;
                        for (int index : hhh.getAllCounters()) {
                            double excess = hhh.getWildcardPattern().getWeight() - sketch.getThreshold();
                            //sketch.getCount(level, index) - sketch.getThreshold();
                            if (excess > 0) {
                                falseProbability *= Math.min(1, sketch.getSum() / (sketch.getCapWidth() * excess));
                            }
                        }

//                        System.out.println(falseProbability + " for " + hhh.getWildcardPattern().getWeight() + " cap width " + sketch.getCapWidth() + " sum " + sketch.getSum());
                        precision += Math.max(0, 1 - falseProbability);
                    }
                }
                sketch.lastAccuracy = precision / outputSize;
            }
        }
    }
}
