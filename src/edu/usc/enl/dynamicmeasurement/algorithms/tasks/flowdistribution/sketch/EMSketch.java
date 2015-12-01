package edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution.sketch;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.flowdistribution.FSAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single.PIRandomGenerator;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.multitask.singleswitch.SingleSwitchTask;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.util.FastMath;
import org.w3c.dom.Element;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/11/2014
 * Time: 5:15 PM <br/>
 * see Kumar, Abhishek, et al. "Data streaming algorithms for efficient and accurate estimation of flow size distribution."
 * ACM SIGMETRICS Performance Evaluation Review 32.1 (2004): 177-188.
 * <br/>
 * This is just one-sized implementation (not multi-resolution)<br/>
 * TODO: Accuracy estimation
 */
public class EMSketch extends FSAlgorithm implements SingleSwitchTask.SingleSwitchTaskImplementation {
    public static final int MAXIMUM_ITERATIONS = 10;
    public static final int MAXIMUM_COLLIDED_ITEMS = 50;
    public static final int MINIMUM_PATTERN_PER_COLLISION_NUM = 10;
    private int[] counts;
    private int hasha, hashb;
    private int capwidth;
    private PrintWriter pw;
    private final static String DISTRIBUTION_TRACE_FORMATTER = "y{%d}{%d}=[%s];";

    public EMSketch(Element element) {
        super(element);
        counts = new int[Util.getNetwork().getFirstMonitorPoints().getCapacity()];
        Random sr = new Random(234212322234l);
        hasha = sr.nextInt() & PIRandomGenerator.MOD;
        hashb = sr.nextInt() & PIRandomGenerator.MOD;
        capwidth = counts.length;
    }

    @Override
    public void reset() {
        Arrays.fill(counts, 0);
    }

    @Override
    public void finish() {
        pw.close();
    }

    public int hash(long x) {
        return PIRandomGenerator.hash31(hasha, hashb, x);
    }

    @Override
    public Map<Long, Long> findFS() {
        int[] usedCounts = Arrays.copyOfRange(counts, 0, capwidth);

        //initial frequency is the same as what we see from counters
        Map<Integer, Long> freq = Arrays.stream(usedCounts).boxed().
                collect(Collectors.groupingBy(Integer::intValue, Collectors.counting()));
        Long zeroNum = freq.get(0);
        if (zeroNum != null && zeroNum == capwidth) {
            return new HashMap<>();
        }

        // estimate the number of items
        int num = (int) (capwidth * Math.log(1.0 * capwidth / zeroNum));

        Distribution newDistribution = new Distribution(freq);
        Distribution oldDistribution = new Distribution();

        // A pattern is a setting of itmes+frequencies that sum to sumI
        // All patterns in this list will map to a common number sumI
        List<Map<Integer, Integer>> patterns = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();
        int iterations = 0;

        //iteratively update the distribution
        //while (notConverged()) {
        while (iterations < MAXIMUM_ITERATIONS) {
            oldDistribution.fillFrom(newDistribution);
            newDistribution.clear();
            for (Map.Entry<Integer, Long> entry : freq.entrySet()) {
                int sumI = entry.getKey();
                int freqI = entry.getValue().intValue();
                if (sumI == 0) {// skip key=0
                    continue;
                }
                //find new probable patterns, get their probabilities and update the distribution
                getPatterns(patterns, sumI, freqI, oldDistribution);
                computeProbabilities(patterns, oldDistribution, probabilities);
                Iterator<Double> probabilityIterator = probabilities.iterator();
                for (Map<Integer, Integer> pattern : patterns) {// for each pattern
                    double probability = probabilityIterator.next();
                    for (Map.Entry<Integer, Integer> patternEntry : pattern.entrySet()) {
                        newDistribution.addFreq(patternEntry.getKey(), freqI * patternEntry.getValue() * probability);
                    }
                }
            }

            //scale factor to make sum of distribution equal to 1
            double scale = num / newDistribution.sumFreq();
            pw.println(String.format(DISTRIBUTION_TRACE_FORMATTER, getStep() + 1, iterations + 1, newDistribution.toString(scale)));
            iterations++;
        }
        pw.flush();
        System.out.println(getStep());

        Map<Long, Long> output = new HashMap<>();
        double freqSum = newDistribution.sumFreq();

        for (Map.Entry<Integer, Double> entry : newDistribution.freq.entrySet()) {
            output.put(entry.getKey().longValue(), (long) (entry.getValue() / freqSum * num));
        }
        return output;
    }

    /**
     * return probable patterns based on oldDistribution that if we add them together it will be sumI.
     * This item is repeated frequencyI items in the distribution.
     * The current algorithm picks at least MINIMUM_PATTERN_PER_COLLISION_NUM for each number of colliding items.
     * Thus it is a kind of two level approach. Even though the patterns with 3 colliding items may be less probable
     * comparing to 2 colliding items, the same number of them will be selected, hopefully we have selected enough
     * out of them.
     *
     * @param patterns        output list
     * @param sumI
     * @param frequencyI
     * @param oldDistribution
     */
    private void getPatterns(List<Map<Integer, Integer>> patterns, int sumI, int frequencyI, Distribution oldDistribution) {
        patterns.clear();
        //int collideNum = getCollideNum(i);
        meanProb = 0;
        sumProb = 0;
        int min = Math.min(sumI, MAXIMUM_COLLIDED_ITEMS);
        for (int j = 0; j < min; j++) { // h=how many items collide with each other
            boolean stop = getPatternRecursive(patterns, j + 1, sumI, 0, 1, 0, new int[j + 1], oldDistribution, MINIMUM_PATTERN_PER_COLLISION_NUM, frequencyI);
            if (stop) {
                break;
            }
        }
    }

    /*
     * for finding the patterns in recursive manner, keeps track of the EWMA mean of the average probabilty of items found
     */
    private double alpha = 0.5;
    private double meanProb = 0;
    private double sumProb = 0;

    /**
     * the heuristic for finding the number of itmes that collide to make an item with key
     *
     * @param key
     * @return
     */
    private int getCollideNum(int key) {
        int collideNum = 0;
        if (key < 50) {
            collideNum = 6;
        } else if (key < 300) {
            collideNum = 4;
        } else if (key < 1000) {
            collideNum = 3;
        } else {
            collideNum = 1;
        }
        collideNum = Math.min(collideNum, key);
        return collideNum;
    }

    /**
     * Fill the list recursively with collideNum numbers that sum to <i>sum</i>.
     * Assuming items already in the array p  sum to <i>usedSum</i>.
     * The numbers must be > min.
     * It stops if it finds <i>limit</i> patterns and the probability of those found patterns become very small
     * comparing to the other patterns found (probability <1/freq)
     *
     * @param patterns
     * @param collideNum
     * @param sum
     * @param usedSum         sum of items in array <i>p</i>
     * @param min:            is used to make p a sorted array
     * @param level           what is the current level of recursion (number of items in array <i>p</i>)
     * @param p               keeps track of already decided items in the pattern, it will be a sorted array
     * @param oldDistribution
     * @param limit           the number of patterns to be found
     * @param freqI           the frequency of the sum items in the sketch
     * @return
     */
    private boolean getPatternRecursive(List<Map<Integer, Integer>> patterns, int collideNum, int sum, int usedSum, int min,
                                        int level, int[] p, Distribution oldDistribution, double limit, int freqI) {
        if (min > sum - usedSum) {//repetitive
            return false;
        }
        if (level == collideNum - 1) { //create the pattern with whatever remained out of sum
            p[level] = sum - usedSum;
            if (oldDistribution.getFreq(p[level]) == 0) {
                return false;
            }
            Map<Integer, Integer> pattern = getPattern(p);
            patterns.add(pattern);
            double prob = getProb(oldDistribution, pattern);
            if (sumProb == 0) {
                meanProb = prob * (level + 1);
            } else {
                meanProb = alpha * meanProb + (1 - alpha) * prob * (level + 1);
            }
            sumProb += prob;
        } else {
            for (int j = min; j <= sum - usedSum - (collideNum - level - 1); j++) {
                p[level] = j;
                if (oldDistribution.getFreq(p[level]) == 0) {
                    continue;
                }
                //if I have found enough patterns and the probability of found items are very small
                if (patterns.size() > limit && meanProb / sumProb < 1.0 / freqI) {
                    return true;
                }
                getPatternRecursive(patterns, collideNum, sum, usedSum + j, Math.max(min, j), level + 1, p, oldDistribution, limit, freqI);
            }
        }
        return false;
    }

    /**
     * Counts the frequency of items in a sorted list
     *
     * @param p is sorted ascending
     * @return
     */
    private Map<Integer, Integer> getPattern(int[] p) {
        Map<Integer, Integer> out = new HashMap<>();
        int count = 0;
        int currentItem = -1;
        for (int item : p) {
            if (currentItem < 0) {
                currentItem = item;
                count++;
            } else if (currentItem != item) {
                out.put(currentItem, count);
                count = 1;
                currentItem = item;
            } else {
                count++;
            }
        }
        if (currentItem > 0) {//put last one
            out.put(currentItem, count);
        }
        return out;
    }

    /**
     * For each pattern in the patterns map computes the probability between 0 and 1 based on the distribution.
     *
     * @param patterns
     * @param distribution
     * @param probabilities the corresponding probabilities of the patterns list
     */
    private void computeProbabilities(List<Map<Integer, Integer>> patterns, Distribution distribution, List<Double> probabilities) {
        double sum = 0;
        probabilities.clear();
        for (Map<Integer, Integer> pattern : patterns) {
            double probability = getProb(distribution, pattern);
            probabilities.add(probability);
            sum += probability;
        }
        //scale to 1
        for (int i = 0, probabilitiesSize = probabilities.size(); i < probabilitiesSize; i++) {
            probabilities.set(i, probabilities.get(i) / sum);
        }
    }

    /**
     * Returns te probability of each of the key items in the pattern.
     * It leverages Poisson distribution. See the paper for its description
     *
     * @param distribution
     * @param pattern
     * @return
     */
    private double getProb(Distribution distribution, Map<Integer, Integer> pattern) {
        return pattern.entrySet().stream().mapToDouble(e -> {
            double l = distribution.getFreq(e.getKey()) / capwidth;
            return l == 0 ? 0 : (new PoissonDistribution(l).probability(e.getValue()) / FastMath.exp(-l));
        }).reduce((a, b) -> (a * b)).getAsDouble();
    }

    private boolean notConverged() {
        return false;
    }

    @Override
    public void doUpdate() {

    }

    @Override
    public void setFolder(String folder) {
        try {
            pw = new PrintWriter(folder + "/iterations.csv");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void match(long key, long size) {
        counts[hash(key) % capwidth] += size;
    }

    @Override
    public void setCapacityShare(int resource) {
        capwidth = resource;
    }

    @Override
    public double estimateAccuracy() {
        return 1;
    }

    @Override
    public int getUsedResourceShare() {
        return 1;
    }

}
