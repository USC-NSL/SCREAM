package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh.sketch.single;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/8/13
 * Time: 5:30 PM
 */
public class LevelHH {
    private double weight;
    private int counterIndex;
    private int[] allCounters;

    public LevelHH(double weight, int counterIndex, int[] allCounters) {
        this.weight = weight;
        this.counterIndex = counterIndex;
        this.allCounters = new int[allCounters.length];
        System.arraycopy(allCounters, 0, this.allCounters, 0, allCounters.length);
    }

    public static int countCollisions(List<LevelHH> hhs, Map<Integer, Integer> usedCounters) {
        int collisions = 0;
        for (LevelHH hh : hhs) {
            Integer integer = usedCounters.get(hh.getCounterIndex());
            if (integer > 1) {
                collisions += integer;
            }
        }
        return collisions;
    }

    public double getWeight() {
        return weight;
    }

    int getCounterIndex() {
        return counterIndex;
    }

    public int[] getAllCounters() {
        return allCounters;
    }

    @Override
    public String toString() {
        return "LevelHH{" +
                "weight=" + weight +
                ", counterIndex=" + counterIndex +
                '}';
    }

    public static void fillUsedCounters(List<LevelHH> hhs, Map<Integer, Integer> usedCounters) {
        for (LevelHH hh : hhs) {
            for (int i : hh.getAllCounters()) {
                Integer usage = usedCounters.get(i);
                if (usage == null) {
                    usage = 0;
                }
                usage++;
                usedCounters.put(i, usage);
            }
        }
    }
}
