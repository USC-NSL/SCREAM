package edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm.cardinlaitycounter;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.superspreader.sketch.hierarchicalcm.CardinalityCounter;
import edu.usc.enl.dynamicmeasurement.data.ConfigReader;
import edu.usc.enl.dynamicmeasurement.util.Util;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 5/7/2014
 * Time: 2:13 PM
 * <br/>
 * from HyperLogLog in Practice: Algorithmic Engineering of a State of The Art Cardinality Estimation Algorithm
 */
public class HyperLogLog implements CardinalityCounter {
    public static final double ALPHA_16 = 0.673;
    public static final double ALPHA_32 = 0.697;
    public static final double ALPHA_64 = 0.709;
    protected final static long MAX_INT = 1L << 32;
    public static final double LARGE_E_THRESHOLD = 1.0 / 32 * MAX_INT;
    private final double alpha;
    protected int p;
    protected int[] counts;
    protected final int m;
    private int hasha, hashb;
    private HashFunction hashFunction;

    public HyperLogLog(Element element, long seed) {
        this(Integer.parseInt(Util.getChildrenProperties(element, "Property").get("Precision").getAttribute(ConfigReader.PROPERTY_VALUE)), seed);
    }

    public HyperLogLog(int precision, long seed) {
        this.p = precision;
        if (p < 4 || p > 16) {
            throw new IllegalArgumentException("Precision must be in range [4,16] but it is " + precision);
        }
        m = 1 << p;
        switch (m) {
            case 16:
                alpha = ALPHA_16;
                break;
            case 32:
                alpha = ALPHA_32;
                break;
            case 64:
                alpha = ALPHA_64;
                break;
            default:
                alpha = 0.7213 / (1 + 1.079 / m);
        }
        counts = new int[m];
        Random sr = new Random(seed);
        hasha = sr.nextInt();
        hashb = sr.nextInt();

        hashFunction = Hashing.murmur3_128(sr.nextInt());
    }

    @Override
    public void match(long item) {
        int h = hash(item);
        int w = h >>> p;
        int idx = h & (m - 1);
//        int lowestOneBit = Integer.highestOneBit(w);
//        int bits = lowestOneBit == 0 ? -1 : (32 - p - (binlog(lowestOneBit)));
        int lowestOneBit = Integer.lowestOneBit(w);
        int bits = lowestOneBit == 0 ? -1 : (binlog(lowestOneBit) + 1);
        counts[idx] = Math.max(bits, counts[idx]);
    }

    @Override
    public String toString() {
        return "HyperLogLog";
    }

    public static int binlog(int bits) // returns 0 for bits=0
    {
        int log = 0;
        if ((bits & 0xffff0000) != 0) {
            bits >>>= 16;
            log = 16;
        }
        if (bits >= 256) {
            bits >>>= 8;
            log += 8;
        }
        if (bits >= 16) {
            bits >>>= 4;
            log += 4;
        }
        if (bits >= 4) {
            bits >>>= 2;
            log += 2;
        }
        return log + (bits >>> 1);
    }

    public int hash(long x) {
        return hashFunction.hashLong(x).asInt();
//        return PIRandomGenerator.hash32(hasha, hashb, x);
    }

    @Override
    public int getCardinality() {
        double c = calculateE();
        if (c < 2.5 * m) {
            int zeros = (int) Arrays.stream(counts).filter(a -> a == 0).count();
            if (zeros > 0) {
                c = linearCounting(zeros);
            }
        } else if (c > LARGE_E_THRESHOLD) {
            c = -MAX_INT * Math.log(1 - c / MAX_INT);
        }
        return (int) Math.round(c);
    }


    protected double calculateE() {
        return alpha * m * m / Arrays.stream(counts).mapToDouble(a -> 1.0 / (1L << a)).sum();
    }

    @Override
    public void reset() {
        Arrays.fill(counts, 0);
    }

    @Override
    public void add(CardinalityCounter cc) {
        for (int i = 0; i < counts.length; i++) {
            counts[i] = Math.max(counts[i], ((HyperLogLog) cc).counts[i]);
        }
    }

    @Override
    public double getRelativeStd() {
        return 1.04 / Math.sqrt(m);
    }

    @Override
    public int getSize() {
        return (int) Math.ceil(m * 5.0 / 32);
    } //assume ordinary counter is 32bits

    public static void main(String[] args) {

        Random sr0 = new Random();
        int[] nums = new int[]{
                10, 20, 30, 40, 50, 60, 70, 80, 90,
                100, 200, 300, 400, 500, 600, 700, 800, 900,
                1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000,
                10000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000};
        int runs = 500;
        int algNum = 5;
        for (int num : nums) {
//        int num = 30000;
            int max = num * 32;
            double[] meanErrors = new double[algNum];
            double[] mean2Errors = new double[algNum];
            List<List<Integer>> errors = new ArrayList<>();
            for (int i = 0; i < algNum; i++) {
                errors.add(new ArrayList<>());
            }
            for (int k = 0; k < runs; k++) {
                Random sr = new Random(sr0.nextLong());
                List<CardinalityCounter> ccs = new ArrayList<>();
                ExactCardinalityCounter groundTruth = new ExactCardinalityCounter();
                ccs.add(groundTruth);
                ccs.add(new HyperLogLog(4, 293749274927l));
                ccs.add(new HyperLogLog(6, 293749274927l));
                ccs.add(new HyperLogLog(8, 293749274927l));
                ccs.add(new HyperLogLog(10, 293749274927l));
                ccs.add(new HyperLogLog(12, 293749274927l));
                for (int i = 0; i < num; i++) {
                    int item = sr.nextInt(max);
                    for (CardinalityCounter cc : ccs) {
                        cc.match(item);
                    }
                }

                for (int i = 0; i < ccs.size() - 1; i++) {
                    CardinalityCounter cc = ccs.get(i + 1);
                    int error = cc.getCardinality() - groundTruth.getCardinality();
                    meanErrors[i] += error;
                    mean2Errors[i] += error * error;
                    errors.get(i).add(error);
//                    System.out.print(error + ",");
                }
//                System.out.println();
            }
            StringBuilder sb = new StringBuilder();
            for (double meanError : meanErrors) {
                sb.append("," + meanError / runs);
            }
            for (List<Integer> er : errors) {
//                System.out.println(er.size());
                Collections.sort(er);
                sb.append("," + (er.get(runs / 2 - 1) + er.get(runs / 2)) / 2);
            }
            for (int i = 0; i < mean2Errors.length; i++) {
                sb.append("," + Math.sqrt(mean2Errors[i] / runs - meanErrors[i] / runs * meanErrors[i] / runs));
            }
            System.out.println(num + sb.toString());
        }
    }

    protected double linearCounting(int zeros) {
        return m * Math.log(1.0 * m / zeros);
    }
}
