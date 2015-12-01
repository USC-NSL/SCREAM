package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.*;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;
import edu.usc.enl.dynamicmeasurement.util.IntegerWrapper;
import edu.usc.enl.dynamicmeasurement.util.LongWrapper;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 6/8/2014
 * Time: 7:00 PM
 */
public class SSHierarchicalCountMin extends AbstractHierarchicalCountMinSketch {
    protected CardinalityCounter[][] counts;
    int[][] countsCache;
    final NormalDistribution normalDistribution = new NormalDistribution();

    private Element cardinalityCounterElement;
    private final boolean estimateRecall = false;
    protected long hhSum = 0;


    double missed_0 = 0;
    double missed_1 = 0;
    double missed_2 = 0;
    double missed_3 = 0;
    private CardinalityCounter justSumForZero;

    public SSHierarchicalCountMin(Element element) {
        super(element);
        Map<String, Element> properties = Util.getChildrenProperties(element, "Property");
        cardinalityCounterElement = properties.get("CardinalityCounter");
        if (cardinalityCounterElement == null) {
            System.err.println("No cardinality counter element in the configuration!");
            return;
        }
        initCounters();
        try {
            justSumForZero = (CardinalityCounter) Class.forName(cardinalityCounterElement.
                    getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class, Long.TYPE).newInstance(cardinalityCounterElement, CardinalityCounter.SEED_CONST);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SSHierarchicalCountMin(double threshold, WildcardPattern taskWildcardPattern1, int depth, int U, int gran,
                                  int[][] hashaa, int[][] hashbb, int capacity, Element cardinalityCounterElement, int aCounterSize) {
        super(threshold, taskWildcardPattern1, depth, U, gran, hashaa, hashbb, capacity, aCounterSize);
        this.cardinalityCounterElement = cardinalityCounterElement;
        try {
            justSumForZero = (CardinalityCounter) Class.forName(cardinalityCounterElement.
                    getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class, Long.TYPE).newInstance(cardinalityCounterElement, CardinalityCounter.SEED_CONST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initCounters();
    }

    @Override
    protected int getACounterSize(Element element) {
        Map<String, Element> properties = Util.getChildrenProperties(element, "Property");
        Element cardinalityCounterElement = properties.get("CardinalityCounter");
        if (cardinalityCounterElement == null) {
            System.err.println("No cardinality counter element in the configuration!");
            return 1;
        }
        return getCCSize(cardinalityCounterElement);
    }


    @Override
    protected void update() {

    }

    public static int getCCSize(Element cardinalityCounterElement) {
        try {
            CardinalityCounter cc = (CardinalityCounter) Class.forName(cardinalityCounterElement.
                    getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class, Long.TYPE).newInstance(cardinalityCounterElement, CardinalityCounter.SEED_CONST);
            return cc.getSize();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    @Override
    public void fillCounters(Collection<? extends AbstractHierarchicalCountMinSketch> subSketches) {
        reset();
        for (AbstractHierarchicalCountMinSketch subSketch : subSketches) {
            CardinalityCounter[][] counts2 = ((SSHierarchicalCountMin) subSketch).counts;
            for (int i = 0; i < counts2.length; i++) {
                CardinalityCounter[] count = counts2[i];
                for (int j = 0; j < count.length; j++) {
                    this.counts[i][j].add(count[j]);
                }
            }
        }
        computeSum();
    }

    int bitCapacity = 0;

    public void setCapacityShare(int c) {
        bitCapacity = c;
        super.setCapacityShare(c / getCCSize());
        if (capWidth == 0) {
            capWidth = 1;
        }
        if (capWidth <= 1) {
            zeroWidth = true;
        } else {
            zeroWidth = false;
        }
    }

    public int getCCSize() {
        return counts[0][0].getSize();
    }

    @Override
    public int getUsedResourceShare() {
        return bitCapacity;
    }

    public void prepareToFindHHH() {
//        System.out.println(lastAccuracy);
        super.prepareToFindHHH();
        for (int i = 0; i < counts.length; i++) {
            CardinalityCounter[] count = counts[i];
            for (int j = 0; j < count.length; j++) {
                countsCache[i][j] = count[j].getCardinality();
            }
        }
        computeSum();
        hhSum = 0;
        if (!zeroWidth) {
            HHHSet result = new HHHSet();
            CMH_recursive(levels, 0, result, new LongWrapper(0), new HashMap<>(), threshold);
            List<HHHOutput> hhhOutputs = result.getResult().get(0);

            if (hhhOutputs != null && hhhOutputs.size() > 0) {
                int[] badCounters = new int[getWidth2() * depth];
                findBadCounters(hhhOutputs, badCounters, 0);
                for (HHHOutput hhh : hhhOutputs) {
                    double weight = hhh.getWildcardPattern().getWeight();
                    if (weight - (badCounters[hhh.getMinIndex()] - weight) > threshold) { //only one used my min counter
                        hhSum += weight;
                    }
                }
                hhSum = Math.min(sum, hhSum);
            }
        }
    }

    @Override
    public Collection<WildcardPattern> findHHH() {
        prepareToFindHHH();
        if (zeroWidth) {
            if (sum >= threshold) {
                lastAccuracy = 0;
            } else {
                lastAccuracy = 1;
            }
            return new ArrayList<>();
        }

        HHHSet result = new HHHSet();
        if (hhSum > 0) {//otherwise nothing is found
            CMH_recursive(levels, 0, result, new LongWrapper(0), precisionEstimationData.getHhTree(), threshold);

            //merge(result, result2);
            if (estimateRecall) {//clean output
                HHHSet result2 = new HHHSet();
                List<HHHOutput> hhhOutputs1 = result2.getResult().get(0);
                if (hhhOutputs1 != null) {
                    for (HHHOutput hhh : hhhOutputs1) {
                        result.add(hhh, 0);
                    }
                }
                result = result2;
            }
        }
        Collection<WildcardPattern> output = fillOutput(result.getResult());
        precisionEstimationData.updateAccuracy(result.getResult(), output.size());
//        System.out.println(getStep() + "," + lastAccuracy);
        accuracyWriter.println(getStep() + "," + lastAccuracy);

        if (estimateRecall) {
            double trueHHH = output.size() * lastAccuracy;
            System.out.println(getStep() + "," + missed_0 + "," + missed_1 + "," +
                    missed_2 + "," + missed_3 + "," + lastAccuracy + "," +
                    trueHHH / (trueHHH + missed_0) + "," + trueHHH / (trueHHH + missed_1) + "," + trueHHH / (trueHHH + missed_2) + "," +
                    trueHHH / (trueHHH + missed_3) + "," + sum);
        }
        return output;


    }

    @Override
    protected void createCounters(int levels) {
        counts = new CardinalityCounter[levels][];
        countsCache = new int[levels][];
    }

    @Override
    protected void createCounterAt(int width, int depth, int i) {
        counts[i] = new CardinalityCounter[width * depth];
        countsCache[i] = new int[width * depth];
        try {
            for (int j = 0; j < counts[i].length; j++) {
                counts[i][j] = (CardinalityCounter) Class.forName(cardinalityCounterElement.
                        getAttribute(ConfigReader.PROPERTY_VALUE)).getConstructor(Element.class, Long.TYPE).newInstance(cardinalityCounterElement, (31 * (i + 1) + ((j / width) + 1)) * CardinalityCounter.SEED_CONST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void CMH_count(int level, long item, CardinalityCounter[] counters) {
        if (isExactCounter(level)) {
            for (int i = 0; i < depth; i++) {
                counters[i] = counts[level][((int) item)];
            }
        } else {
            int offset = 0;
            for (int j = 0; j < this.depth; j++) {
                counters[j] = counts[level][(PIRandomGenerator.hash31(hasha[level][j], hashb[level][j], item) % capWidth) + offset];
                offset += getWidth2(); //important to be width
            }
        }
    }

    @Override
    public long CMH_count(int level, long item, IntegerWrapper indexW, int[] indexArray) {
        if (level >= levels) {
            return (sum);
        }
        if (zeroWidth) {
            indexW.setValue(0);
            return sum;
        }

        int[] counters = countsCache[level];
        if (isExactCounter(level)) { // use an exact count if there is one
            int item1 = (int) item;
            if (indexW != null) {
                indexW.setValue(item1);
            }
            Arrays.fill(indexArray, 0);
            indexArray[depth / 2] = item1;
            return counters[item1];
        }

        int offset = 0;
        for (int j = 0; j < this.depth; j++) {
            indexArray[j] = (PIRandomGenerator.hash31(hasha[level][j], hashb[level][j], item) % capWidth) + offset;
            offset += getWidth2(); //important to be width
        }
        long l = estimateMedian(level, indexArray, counters);
        if (indexW != null) {
//            indexW.setValue(indexArray[indexArray.length-1]); //the smaller one (sorted descendingly)
            indexW.setValue(indexArray[depth / 2]); //the smaller one (sorted descendingly)
        }
        return l;
    }

    protected long getMin(int[] counters, int[] indexes) {
        long min = Long.MAX_VALUE;
        for (int index : indexes) {
            min = min < counters[index] ? min : counters[index];
        }
        return min;
    }

    @Override
    public int getCount(int level, int index) {
        return countsCache[level][index];
    }

    protected long estimateMedian(int level, int[] indexArray, int[] counters) {
//        return getMin(counters, indexArray);
        sort(counters, indexArray);
        long output;
        if (depth % 2 == 0) {
            output = (long) (counters[indexArray[depth / 2 - 1]] + counters[indexArray[depth / 2]]) / 2;
        } else {
            output = (long) counters[indexArray[depth / 2]];
        }

//        return output;
        return unbias(output, level);
    }

    protected long unbias(long output, int level) {
        if (level == wildcardNum) {
            //biassed=u+(s-h-u)/w
            return Math.max(0, (capWidth * output - (sum - hhSum)) / (capWidth - 1));
//            return Math.max(0, (capWidth * output - (sum )) / (capWidth - 1));
        } else {
            return output;
        }
    }

    public static void sort(int[] count, int[] indexArray) {
        for (int i = 0; i < indexArray.length - 1; i++) {
            int max = count[indexArray[i]];
            int maxIndex = i;
            for (int j = i + 1; j < indexArray.length; j++) {
                if (max < count[indexArray[j]]) {
                    max = count[indexArray[j]];
                    maxIndex = j;
                }
            }

            //swap
            if (maxIndex != i) {
                int temp = indexArray[i];
                indexArray[i] = indexArray[maxIndex];
                indexArray[maxIndex] = temp;
            }
        }
    }


    @Override
    public void match(long item, double diff2) {
        int i, j, offset;
        item = realToCounterInput(item);
        long diff = (long) diff2;
        if (!zeroWidth) {
            for (i = 0; i < levels; i++) {
                offset = 0;
                if (isExactCounter(i)) {
                    counts[i][(int) item].match(diff); //reaching here item is small enough
                    // keep exact counts at high levels in the hierarchy
                } else {
                    for (j = 0; j < depth; j++) {
                        counts[i][(PIRandomGenerator.hash31(hasha[i][j], hashb[i][j], item)
                                % capWidth) + offset].match(diff);
                        // this can be done more efficiently if the width is a power of two
                        offset += getWidth2();//this is important to be width not cap width
                    }
                }
                item >>>= gran;
            }
        } else {
            justSumForZero.match(diff);
        }
    }

    @Override
    public void reset() {
        super.reset();
        for (CardinalityCounter[] count : counts) {
            if (count != null) {
                for (CardinalityCounter cardinalityCounter : count) {
                    cardinalityCounter.reset();
                }
            }
        }
        Arrays.stream(countsCache).forEach(a -> Arrays.fill(a, 0));
        justSumForZero.reset();
        hhSum = 0;
        missed_0 = 0;
        missed_1 = 0;
        missed_2 = 0;
        missed_3 = 0;
    }

    @Override
    protected PrecisionEstimationData MakePrecisionEstimationData() {
        return new RecallAndPrecisionEstimationData(this, estimateRecall);
    }

    @Override
    protected void computeSum() {
        //TODO: Fix
        //it can be different inter-layers
        //it can be different intra-layers among multiple hash functions
        if (zeroWidth) {
            sum = justSumForZero.getCardinality();
        } else {
            sum = Arrays.stream(countsCache[0]).sum() / getDepth();
        }
//            CardinalityCounter[] count = counts[0];
//            sum = Arrays.stream(count).mapToInt(CardinalityCounter::getCardinality).reduce(0, (a, b) -> a + b) / getDepth();
    }

    @Override
    public double getFalseProbability(double newSum, HHHOutput hhhOutput, int level, int[] badCounters) {
        double threshold2 =
                //threshold;
                ((capWidth - 1) * threshold + (sum - hhSum)) / capWidth;
        double CMMean = newSum / capWidth / threshold2;
        double falseProbability = 1;
        int[] allCounters = hhhOutput.getAllCounters();
//        for (int i = 0; i < allCounters.length / 2 + 1; i++) { // no need for this, it is better to look at all counters
        for (int index : allCounters) {
            int weight = countsCache[level][index];
//            if (weight > threshold) {
            double v = badCounters == null ? 0 : (badCounters[index] - hhhOutput.getWildcardPattern().getWeight());
            // bad counters is the weight of all hhs - my weight it will be other hhs have collision on this counter
            double margin = (weight - threshold2 - v) / threshold2;
            if (margin > 0) { //still <0 guys can help
                falseProbability *= Math.min(1, computeFalseIntegrate(margin, CMMean));
            } else {
                falseProbability *= Math.min(1, 0.5 + computeFalseIntegrate(margin, CMMean));
            }

//            }
        }
//        System.out.println("step "+getStep()+" hh "+ hhhOutput.getWildcardPattern().toString()+" "+falseProbability);
        return falseProbability;
    }

    public boolean filterLevel(int level) {
        return level == wildcardNum || estimateRecall;
    }


    /**
     * This is for recall
     *
     * @param level
     * @param start
     * @param resSize
     * @param hhTree
     * @param threshold
     * @return the total weight of found HHHs
     */
    protected long CMH_recursive2(int level, long start, IntegerWrapper resSize,
                                  Map<Integer, Double> hhTree, double threshold, int missLevel, double[] missProbabilities) {
        // for finding heavy hitters, recursively descend looking
        // for ranges that exceed the threshold
        int recallLevel = 6;

        int i;
        int blockSize;
        long estcount;
        long itemShift;
        double maxHHH = 2 * Math.ceil(sum / threshold);
        estcount = CMH_count(level, start);
//            boolean doPrint=false;
//            if ((new WildcardPattern(start,level,0).match(Long.parseLong("00101000000010101011100000011100",2)) ||new WildcardPattern(start,level,0).match(Long.parseLong("00101000000010101011111110010100",2)) ||new WildcardPattern(start,level,0).match(Long.parseLong("00111000001011011101101011011110",2)) ) && getStep()==115){
//                doPrint=true;
//            }
        if (estcount >= threshold) {
            if (missLevel > 0) {
                Double levelHHs = hhTree.get(level);
                if (levelHHs == null) {
                    levelHHs = 0d;
                }
                if (levelHHs > sum) {
                    levelHHs = (double) sum;
                }
                double missProbability = 0;
//                        if (estcount - threshold - (sum - levelHHs) / capWidth > 0) {
//                missProbability = normalDistribution.cumulativeProbability((estcount - threshold - (sum - levelHHs) / capWidth) / threshold / counts[0][0].getRelativeStd());

                double margin = (estcount - threshold) / threshold;
                double CMMean = (sum - levelHHs) / capWidth / threshold;
                double p = 1 - Math.pow(computeFalseIntegrate(margin, CMMean), depth / 2);     //FIXME
//                    System.out.println(p);
//                        }
                missProbabilities[missLevel] = p; //missProbability;
                if (level == wildcardNum || missLevel == recallLevel) {
                    double probability = sumMissProbabilities(missLevel, missProbabilities);
//                        System.out.println(">" + String.format("%.5f", probability) + "," + String.format("%.5f", missProbability) + "," + (Long.toBinaryString((1l << 32) + (start << level))).substring(1, (levels - level + 1)));
                    missed_3 += probability;
                    return 0;
                }
                missLevel++;
            }
            long descendantHHHSum = 0;
            if (level > 0) {
                IntegerWrapper childrenResSize = new IntegerWrapper(0);
                blockSize = 1 << gran;
                itemShift = start << gran;
                // assumes that gran is an exact multiple of the bit dept
                for (i = 0; i < blockSize; i++) {
                    if (resSize.getValue() + childrenResSize.getValue() > maxHHH) {
                        break;
                    }
                    descendantHHHSum += CMH_recursive2(level - 1, itemShift + i, childrenResSize, hhTree, threshold, missLevel, missProbabilities);
                }
                resSize.add(childrenResSize.getValue());
            }

            if (estcount - descendantHHHSum >= threshold) {
                if (resSize.getValue() < maxHHH) { // WHY? Haha now I understand because it can use all memory
                    resSize.add(1);
                }
                return estcount;
            }
            return descendantHHHSum;
        } else {//to estimate recall
            Double levelHHs = hhTree.get(level);
            if (levelHHs == null) {
                levelHHs = 0d;
            }
            if (levelHHs > sum) {
                levelHHs = (double) sum;
            }

//                int hh = 0;
//                int small = 0;
//                for (Integer c : countsCache[level]) {
//                    if (c >= threshold) {
//                        hh++;
//                    } else if (c <= estcount) {
//                        small++;
//                    }
//                }
//                double baysCoef = 1.0 * hh / small;

            double sigma = getCCSigma();
            double missProbability = normalDistribution.cumulativeProbability((estcount - threshold - (sum - levelHHs) / capWidth) / threshold / sigma);

            double margin = (estcount - threshold) / threshold;
            double CMMean = (sum - levelHHs) / capWidth / threshold;
            missProbability = 1 - Math.pow(computeFalseIntegrate(margin, CMMean), depth / 2); //FIXME

            if (missLevel == 0) {
                missed_0 += normalDistribution.cumulativeProbability((estcount - threshold) / threshold / sigma);
                missed_1 += normalDistribution.cumulativeProbability((estcount - threshold - sum / capWidth) / threshold / sigma);
                missed_2 += missProbability;
            }
            missProbabilities[missLevel] = missProbability;

            if (missLevel < recallLevel && level > 0 && missProbability >= 0.05) {
                blockSize = 1 << gran;
                itemShift = start << gran;
                IntegerWrapper childrenResSize = new IntegerWrapper(0);
                for (i = 0; i < blockSize; i++) {
                    CMH_recursive2(level - 1, itemShift + i, childrenResSize, hhTree, threshold, missLevel + 1, missProbabilities);
                }
            }
            if (missProbability < 0.05 && missLevel > 0) {
//                    System.out.println("<" + String.format("%.5f", missProbability) + "," + String.format("%.5f", missProbability) + "," + (Long.toBinaryString((1l << 32) + (start << level))).substring(1, (levels - level + 1)));
                missed_3 += missProbability;
            }
            if (level == wildcardNum || missLevel == recallLevel) {
//                    missProbabilities[missLevel] = missProbability;
//                    double probability = 0;
//                    for (int j = 0; j < missLevel + 1; j++) {
//                        probability += missProbabilities[j];
//                    }
//                    probability/=(missLevel+1);
//                    System.out.println("<" + String.format("%.5f", missProbability) + "," + String.format("%.5f", missProbability) + "," + (Long.toBinaryString((1l << 32) + (start << level))).substring(1, (levels - level + 1)));
                missed_3 += missProbability;
            }

        }
        return 0;
    }

    private double sumMissProbabilities(int missLevel, double[] missProbabilities) {
        double probability = 0;
        for (int j = 0; j < missLevel + 1; j++) {
            probability += missProbabilities[j];
        }
        probability /= (missLevel + 1);
        return probability;
    }

    public double computeFalseIntegrate(double margin, double CMMean) {
        return computeFalseIntegrate2(margin, CMMean, getCCSigma(), normalDistribution);
    }

    public static double computeFalseIntegrate2(double margin, double CMMean, double std, NormalDistribution normalDistribution) {
        int probGranularity = 10;
        double p = 0;
        double step = 2.0 * std / probGranularity;
        if (margin > 0) {
            for (int j = -probGranularity; j < 0; j++) {
                double t = step * (j + 0.5);
                double v = margin - t > 0 ? Math.min(1, CMMean / (margin - t)) : 1;
                double bound = j == -probGranularity ? -100 : (step * j / std);
                double normalProbability = normalDistribution.probability(bound, step * (j + 1) / std);
                p += normalProbability * v;
            }
            step = margin / probGranularity;
            for (int j = 0; j < probGranularity; j++) {
                double t = step * (j + 0.5);
                double v = margin - t > 0 ? Math.min(1, CMMean / (margin - t)) : 1;
                double normalProbability = normalDistribution.probability(step * j / std, step * (j + 1) / std);
                p += normalProbability * v;
            }
        } else {
            for (int j = 0; j < probGranularity; j++) {
                double t = step * (j + 0.5);
                double v = t + margin > 0 ? Math.min(1, CMMean / (t + margin)) : 1;
                double bound = j + 1 == probGranularity ? 100 : (step * (j + 1) / std);
                double normalProbability = normalDistribution.probability(step * j / std, bound);
                p += normalProbability * v;
            }
        }
        return p;
    }

    public double getCCSigma() {
        return counts[0][0].getRelativeStd();
    }

    protected class SSFinder extends HHFinder {
        public SSFinder() {
            super(SSHierarchicalCountMin.this);
        }

        @Override
        protected double unBiasHHSum(double hhSum, int count) {
            return hhSum;
        }

        protected double getHHSumAndCollisions(TreeMap<Integer, List<LevelHH>> hhTree, Map<Integer, Double> currentLevelHhSum,
                                               Map<Integer, Double> currentLevelHhCollision) {
            //need to increase totalSum as return value
            super.getHHSumAndCollisions(hhTree, currentLevelHhSum, currentLevelHhCollision);
            double totalSum = 0;
            for (List<LevelHH> levelHHs : hhTree.values()) {
                for (LevelHH levelHH : levelHHs) {
                    totalSum += (capWidth * levelHH.getWeight() + sum - hhSum) / (capWidth + 1); //reverse the bias correction in CMH_count
                }
            }
            return totalSum;
        }

        protected boolean thresholdCondition(double currentThreshold, long sketchSum) {
            return currentThreshold > (sketchSum - hhSum) / getCapWidth() / 2;
        }
    }
}
