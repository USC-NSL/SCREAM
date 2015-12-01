package edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh2d.groundtruth;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh2d.HHH2DAlgorithm;
import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh2d.WildcardPatternND;
import org.w3c.dom.Element;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/2/2014
 * Time: 5:02 AM
 */
public class HHH2DGroundTruth extends HHH2DAlgorithm {
    private Map<KeyTuple, Double> data = new HashMap<>();
    private KeyTuple tempKeyTuple = new KeyTuple(0, 0);

    public HHH2DGroundTruth(Element element) {
        super(element);
    }

    @Override
    public void reset() {
        data.clear();
    }

    @Override
    public void finish() {

    }

    // make sure of diamond effect
    @Override
    public Collection<WildcardPatternND> findHHH() {
        List<WildcardPatternND> output = new ArrayList<>();
        List<WildcardPatternND> tempOutput = new ArrayList<>();

        //init
        LatticeNode[][] nodes = new LatticeNode[33][33];
        nodes[32][32] = new LatticeNode(0, 0);
        nodes[32][32].putAll(data);

        for (int level = 32 + 32; level >= 0; level--) { //for each level
            for (int i = Math.min(32, level); level - i <= 32 && i >= 0; i--) {
                int j = level - i;
//                System.out.println(i + "," + j);
                nodes[i][j].removeMatching(output);
                nodes[i][j].findHHH(data, tempOutput);
                output.addAll(tempOutput);
                tempOutput.clear();
                if (i - 1 >= 0) {
                    if (nodes[i - 1][j] == null) {
                        nodes[i - 1][j] = new LatticeNode(32 - (i - 1), 32 - j);
                    }
                    nodes[i - 1][j].putAll(nodes[i][j].items);
                }
                if (j - 1 >= 0) {
                    if (nodes[i][j - 1] == null) {
                        nodes[i][j - 1] = new LatticeNode(32 - i, 32 - (j - 1));
                    }
                    nodes[i][j - 1].putAll(nodes[i][j].items);
                }
                nodes[i][j] = null;
            }
        }
        return output;
    }

    @Override
    public void doUpdate() {

    }

    @Override
    public void setFolder(String folder) {

    }

    @Override
    public void match(long key1, long key2, double value) {
        if (value <= 0) {
            return;
        }
        tempKeyTuple.setKey1(key1);
        tempKeyTuple.setKey2(key2);
        updateMap(value, data, tempKeyTuple);
    }

    protected void updateMap(double value, Map<KeyTuple, Double> map, KeyTuple keyTuple) {
        Double value2 = map.get(keyTuple);
        if (value2 == null) {
            map.put(keyTuple.clone(), value);
        } else {
            map.put(keyTuple, value + value2);
        }
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

    private class LatticeNode {
        Map<KeyTuple, Double> items = new HashMap<>();
        private final int length1;
        private final int length2;

        private LatticeNode(int length1, int length2) {
            this.length1 = length1;
            this.length2 = length2;
        }

        public void putAll(Map<KeyTuple, Double> map) {
            items.putAll(map);
        }

        public void put(KeyTuple key, double value) {
            items.put(key, value);
        }

        @Override
        public String toString() {
            return length1 + "," + length2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LatticeNode that = (LatticeNode) o;

            if (length1 != that.length1) return false;
            if (length2 != that.length2) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = length1;
            result = 31 * result + length2;
            return result;
        }

        public void findHHH(Map<KeyTuple, Double> temp, List<WildcardPatternND> output) {
            temp.clear();
            for (Map.Entry<KeyTuple, Double> entry : items.entrySet()) {
                KeyTuple key = entry.getKey();
                tempKeyTuple.setKey1(key.getKey1() >>> length1);
                tempKeyTuple.setKey2(key.getKey2() >>> length2);
                updateMap(entry.getValue(), temp, tempKeyTuple);
            }
            //find HHH
            temp.entrySet().stream().filter(entry -> entry.getValue() >= threshold).forEach(entry -> {
                KeyTuple key = entry.getKey();
                output.add(new WildcardPatternND(new long[]{key.getKey1(), key.getKey2()}, new int[]{length1, length2}, entry.getValue()));
            });
//            for (Iterator<Map.Entry<KeyTuple, Double>> iterator = items.entrySet().iterator(); iterator.hasNext(); ) {
//                Map.Entry<KeyTuple, Double> next = iterator.next();
//                for (WildcardPatternND hhh : output) {
//                    if (match(hhh, next.getKey())) {
//                        iterator.remove();
//                        break;
//                    }
//                }
//            }
        }

        private boolean match(WildcardPatternND hhh, KeyTuple key) {
            return hhh.match(key.getKey1(), 0) && hhh.match(key.getKey2(), 1);
        }

        public void removeMatching(List<WildcardPatternND> output) {
            for (Iterator<Map.Entry<KeyTuple, Double>> iterator = items.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<KeyTuple, Double> next = iterator.next();
                for (WildcardPatternND hhh : output) {
                    if (match(hhh, next.getKey())) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    private static class KeyTuple implements Cloneable {
        long key1;
        long key2;

        private KeyTuple(long key1, long key2) {
            this.key1 = key1;
            this.key2 = key2;
        }

        public long getKey1() {
            return key1;
        }

        public long getKey2() {
            return key2;
        }

        @Override
        public String toString() {
            return "keyTuple{" +
                    "key1=" + key1 +
                    ", key2=" + key2 +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            KeyTuple keyTuple = (KeyTuple) o;

            if (key1 != keyTuple.key1) return false;
            if (key2 != keyTuple.key2) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (key1 ^ (key1 >>> 32));
            result = 31 * result + (int) (key2 ^ (key2 >>> 32));
            return result;
        }

        //not use after inserting in a hash table
        public void setKey1(long key1) {
            this.key1 = key1;
        }

        //not use after inserting in a hash table
        public void setKey2(long key2) {
            this.key2 = key2;
        }

        @Override
        protected KeyTuple clone() {
            return new KeyTuple(key1, key2);
        }
    }
}
