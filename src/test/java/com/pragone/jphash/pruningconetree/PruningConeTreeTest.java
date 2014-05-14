package com.pragone.jphash.pruningconetree;

import com.pragone.jphash.index.Query;
import com.pragone.jphash.index.Vector;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 1/05/2014
 * Time: 8:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class PruningConeTreeTest {

    private static final int DIMENSIONS = 40;

    @Test
    public void testAdd() {
        PruningConeTree tree = new PruningConeTree();
        tree.add(getRandomVector(DIMENSIONS, 100));
    }


    @Test
    public void testSearchInLowDimensions() {
        int nodeSize = 10;
        PruningConeTree tree = new PruningConeTree(Math.PI/4, 0.5, 10);
        //List<double[]> vectors = populateRandom(tree, 30, 3, 100);
        //printVectors(vectors);
        List<double[]> vectors = new ArrayList<double[]>();
        vectors.add(new double[] {77.70176498116555, 182.6366419604867, 184.452422671186});
        vectors.add(new double[] {90.13444552156517, 156.90522877473836, 153.23459682303144});
        vectors.add(new double[] {70.43588291695296, 144.8240112865408, 115.7531937610102});
        vectors.add(new double[] {23.349186085639374, 113.19096637157797, 187.54977875948117});
        vectors.add(new double[] {31.04071146108659, 110.78655892809655, 112.92078923295179});
        vectors.add(new double[] {72.22910855217609, 172.27772491706648, 110.32037633402018});
        vectors.add(new double[] {94.34170872652678, 102.45934588283333, 149.4878045057245});
        vectors.add(new double[] {105.68702732458162, 166.25938221913952, 162.19513156527415});
        vectors.add(new double[] {46.58118264957257, 151.06725028011198, 180.6410164182393});
        vectors.add(new double[] {71.69344310865992, 107.26092561097482, 171.57921708069});
        vectors.add(new double[] {25.038714451813615, 138.10223915595077, 164.27147252323573});
        vectors.add(new double[] {38.97679317048615, 150.82083709699555, 152.51262540208901});
        vectors.add(new double[] {61.81117130728214, 143.44251179438444, 111.95605794091571});
        vectors.add(new double[] {77.90073124668673, 112.97005024793842, 125.74635215730162});
        vectors.add(new double[] {46.544915711859844, 173.01871199807874, 179.77932460899223});
        vectors.add(new double[] {37.585176293531845, 173.7705404031444, 126.61341696436185});
        vectors.add(new double[] {44.34670084088936, 159.48843542649, 123.10315924282001});
        vectors.add(new double[] {15.832494787392415, 153.46597548386046, 145.93231494634745});
        vectors.add(new double[] {41.91796947788818, 150.18595095888628, 186.68982929279534});
        vectors.add(new double[] {67.72330669889762, 140.03793569584553, 138.6252847051797});
        vectors.add(new double[] {100.61718468456104, 180.15787518063735, 145.7509501754609});
        vectors.add(new double[] {103.65857401807077, 137.93601237109522, 182.5964986012226});
        vectors.add(new double[] {49.05077320626245, 160.80311686168446, 179.23778086485714});
        vectors.add(new double[] {32.384171323896254, 92.07817287500691, 104.74635493288034});
        vectors.add(new double[] {40.257302271967774, 131.37008678729097, 154.65153976674605});
        vectors.add(new double[] {76.81703020188893, 174.85617081237018, 137.2859753338546});
        vectors.add(new double[] {55.85123992860376, 129.62679067417406, 155.41835621128808});
        vectors.add(new double[] {46.41113193518079, 148.72396506481599, 174.68655198958726});
        vectors.add(new double[] {48.83904528997557, 146.14809434485014, 144.96119986350794});
        vectors.add(new double[] {78.90816781548723, 85.8272470132913, 167.01441870496808});
        for (double[] vector: vectors) {
            tree.add(vector);
        }
//        double[] queryVector = getRandomVector(3, 100);
//        System.out.println("Query: " + (new Vector(queryVector)));
        double[] queryVector = {38.76117074251793, 78.43972538522617, 55.35981227709609};
        Vector q = new Vector(queryVector);
        double maxDot = Double.MIN_VALUE;
        double[] maxVec = null;
        for (double[] vector : vectors) {
            double dotproduct =  q.similarity(new Vector(vector));
            if (dotproduct > maxDot) {
                maxDot = dotproduct;
                maxVec = vector;
            }
        }

        Vector resp = tree.query(queryVector);
        Assert.assertArrayEquals(resp.getCoords(),maxVec, 0.0001);
    }

    @Test
    public void testSimpleHierarchicalSearch() {
        int nodeSize = 10;
        PruningConeTree tree = new PruningConeTree();
        List<double[]> vectors = populateRandom(tree, 20, DIMENSIONS, 100);
        double[] queryVector = getRandomVector(DIMENSIONS, 100);
        Vector q = new Vector(queryVector);
        double maxDot = Double.MIN_VALUE;
        double[] maxVec = null;
        for (double[] vector : vectors) {
            double dotproduct =  q.similarity(new Vector(vector));
            if (dotproduct > maxDot) {
                maxDot = dotproduct;
                maxVec = vector;
            }
        }

        Vector resp = tree.query(queryVector);
        Assert.assertArrayEquals(resp.getCoords(),maxVec, 0.0001);
    }

    @Test
    public void testBigHierarchicalSearch() {
        PruningConeTree tree = new PruningConeTree();
        int totalNodes = 100000;
        List<double[]> vectors = new ArrayList<double[]>(totalNodes);
        for (int i = 0; i < 10; i++) {
            vectors.addAll(populateRandom(tree, (int) totalNodes/10, DIMENSIONS, 100, getRandomVector(DIMENSIONS, 500)));
        }
        Vector queryVector = new Vector(getRandomVector(DIMENSIONS, 700));

        double maxDot = Double.MIN_VALUE;
        double[] maxVec = null;
        for (double[] vector : vectors) {
            double dotproduct = queryVector.similarity(new Vector(vector));
            if (dotproduct > maxDot) {
                maxDot = dotproduct;
                maxVec = vector;
            }
        }

        Query query = new Query(queryVector);
        Vector resp = tree.query(query);

        Assert.assertArrayEquals(resp.getCoords(),maxVec, 0.0001);
    }

    private List<double[]> populateRandom(PruningConeTree tree, int howMany, int dimensions, int max) {
        return populateRandom(tree, howMany, dimensions, max, getRandomVector(dimensions,max));
    }
    private List<double[]> populateRandom(PruningConeTree tree, int howMany, int dimensions, int max, double[] center) {
        List<double[]> vectors = new ArrayList<double[]>();
        for (int i = 0; i < howMany; i++) {
            double[] vector = getRandomVector(dimensions, max, center);
            vectors.add(vector);
            tree.add(vector);
        }
        return vectors;
    }

    private void printVectors(List<double[]> vectors) {
        for(double[] vector : vectors) {
            System.out.println(new Vector(vector));
        }
    }

    private double[] getRandomVector(int size, int max, double[] center) {
        double[] temp = new double[size];
        for(int i = 0;  i < size; i++) {
            temp[i] = max*Math.random() + (center != null ? center[i] : 0);
        }
        return temp;
    }

    private double[] getRandomVector(int size, int max) {
        return getRandomVector(size,max,null);
    }
}
