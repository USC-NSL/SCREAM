package edu.usc.enl.dynamicmeasurement.ui;

import edu.usc.enl.dynamicmeasurement.algorithms.tasks.hhh2d.WildcardPatternND;
import edu.usc.enl.dynamicmeasurement.model.WildcardPattern;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: masoud
 * Date: 9/2/2014
 * Time: 11:28 AM
 */
public class DrawBoxes extends JFrame {

    public static final int SIZE_SHIFT = 32 - 9;
    private static int size = 512;

    public static void main(String[] args) {
        String fileName = args[0];
        List<List<WildcardPatternND>> wildcardPatternNDList = new ArrayList<>();
        readFile(fileName, wildcardPatternNDList);
        DrawBoxes frame = new DrawBoxes(wildcardPatternNDList);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(size, size));
        frame.setVisible(true);
    }

    public DrawBoxes(List<List<WildcardPatternND>> input) throws HeadlessException {
        int time = 0;
        double transparencyScale = Double.MAX_VALUE;
        for (WildcardPatternND wp : input.get(time)) {
            transparencyScale = Math.min(transparencyScale, (getSize(wp, 0) * getSize(wp, 1)));
            System.out.println(wp.toCIDRString());
        }
        final double toPassTransparencyScale = transparencyScale;
        JPanel contentPane = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Graphics2D g2 = (Graphics2D) g;
                for (WildcardPatternND wp : input.get(time)) {
//                    float transparency = (float) (toPassTransparencyScale / (DrawBoxes.this.getSize(wp, 0) * DrawBoxes.this.getSize(wp, 1)));
                    float transparency = (float) (0.20 + 0.80 * Math.exp(-((DrawBoxes.this.getSize(wp, 0) * DrawBoxes.this.getSize(wp, 1)) / toPassTransparencyScale - 1)));
                    g2.setColor(new Color(1f, 0f, 0f, transparency));
                    System.out.println(getCoordinate(wp, 0) + "," + getCoordinate(wp, 1) + "," + DrawBoxes.this.getSize(wp, 0) + "," +
                            DrawBoxes.this.getSize(wp, 1) + "," + transparency);
                    g2.fillRect(getCoordinate(wp, 0), getCoordinate(wp, 1), DrawBoxes.this.getSize(wp, 0), DrawBoxes.this.getSize(wp, 1));
                    g2.setColor(Color.black);
                    g2.drawRect(getCoordinate(wp, 0), getCoordinate(wp, 1), DrawBoxes.this.getSize(wp, 0), DrawBoxes.this.getSize(wp, 1));
                }
            }
        };
        this.setContentPane(contentPane);
    }

    protected int getSize(WildcardPatternND wp, int dim) {
        return ((int) ((1l << wp.getDim(dim).getWildcardNum()) >> SIZE_SHIFT));
    }

    protected int getCoordinate(WildcardPatternND wp, int dim) {
        WildcardPattern dim1 = wp.getDim(dim);
        return ((int) ((dim1.getData() << dim1.getWildcardNum()) >> SIZE_SHIFT));
    }

    protected static void readFile(String fileName, List<List<WildcardPatternND>> wildcardPatternNDList) {
        int lastStep = -1;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            while (br.ready()) {
                String line = br.readLine();
                int timeComma = line.indexOf(",");
                int step = Integer.parseInt(line.substring(0, timeComma));
                if (step != lastStep) {
                    lastStep = step;
                    wildcardPatternNDList.add(new ArrayList<>());
                }
                wildcardPatternNDList.get(wildcardPatternNDList.size() - 1).add(new WildcardPatternND(line.substring(timeComma + 1)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
