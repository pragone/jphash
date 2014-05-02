package com.pragone.jphash.iptree;

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
public class IPTreeTest {

    private static final int DIMENSIONS = 40;

    @Test
    public void testAdd() {
        IPTree tree = new IPTree(10,DIMENSIONS);
        tree.add(getRandomVector(DIMENSIONS, 100));
    }

    @Test
    public void testNodeSplitAfterMaxNodeSize() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize,DIMENSIONS);
        populateRandom(tree,nodeSize,DIMENSIONS,100);
        Assert.assertTrue(tree.isLeaf());
        tree.add(getRandomVector(DIMENSIONS,100));
        Assert.assertFalse(tree.isLeaf());
    }

    @Test
    public void testVectorArrayClearedOnSplit() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize, DIMENSIONS);
        populateRandom(tree,nodeSize+1,DIMENSIONS,100);
        Assert.assertNull(tree.vectors);
    }

    @Test
    public void testNoVectorLostOnSplit() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize, DIMENSIONS);
        populateRandom(tree,15,DIMENSIONS,100);
        Assert.assertEquals(15, tree.left.getNumVectorsRecursive() + tree.right.getNumVectorsRecursive());
    }

    @Test
    public void testLinearSearch() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize, DIMENSIONS);
        List<double[]> vectors = populateRandom(tree, 10, DIMENSIONS, 100);
        double[] queryVector = getRandomVector(DIMENSIONS, 100);
        double[] resp = tree.query(queryVector);

        double maxDot = Double.MIN_VALUE;
        double[] maxVec = null;
        for (double[] vector : vectors) {
            double dotproduct = IPTree.dotProduct(vector,queryVector);
            if (dotproduct > maxDot) {
                maxDot = dotproduct;
                maxVec = vector;
            }
        }

        Assert.assertArrayEquals(resp,maxVec, 0.0001);
    }


    @Test
    public void testSimpleHierarchicalSearch() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize, DIMENSIONS);
        List<double[]> vectors = populateRandom(tree, 20, DIMENSIONS, 100);
        double[] queryVector = getRandomVector(DIMENSIONS, 100);

        double maxDot = Double.MIN_VALUE;
        double[] maxVec = null;
        for (double[] vector : vectors) {
            double dotproduct = IPTree.dotProduct(vector,queryVector);
            if (dotproduct > maxDot) {
                maxDot = dotproduct;
                maxVec = vector;
            }
        }

        double[] resp = tree.query(queryVector);
        Assert.assertArrayEquals(resp,maxVec, 0.0001);
    }

    @Test
    public void testBigHierarchicalSearch() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize, DIMENSIONS);
        int totalNodes = 100000;
        List<double[]> vectors = new ArrayList<double[]>(totalNodes);
        for (int i = 0; i < 10; i++) {
            vectors.addAll(populateRandom(tree, (int) totalNodes/10, DIMENSIONS, 100, getRandomVector(DIMENSIONS, 500)));
        }
        double[] queryVector = getRandomVector(DIMENSIONS, 700);

        double maxDot = Double.MIN_VALUE;
        double[] maxVec = null;
        for (double[] vector : vectors) {
            double dotproduct = IPTree.dotProduct(vector,queryVector);
            if (dotproduct > maxDot) {
                maxDot = dotproduct;
                maxVec = vector;
            }
        }

        IPTree.QueryStats stats = new IPTree.QueryStats();
        double[] resp = tree.query(queryVector, stats);

        long start = System.currentTimeMillis();
        tree.rebalance();
        System.out.println("Rebalance done in " + (System.currentTimeMillis() - start));
        IPTree.QueryStats stats2 = new IPTree.QueryStats();
        double[] resp2 = tree.query(queryVector, stats2);
        Assert.assertArrayEquals(resp,maxVec, 0.0001);
    }
    private List<double[]> populateRandom(IPTree tree, int howMany, int dimensions, int max) {
        return populateRandom(tree, howMany, dimensions, max, getRandomVector(dimensions,max));
    }
    private List<double[]> populateRandom(IPTree tree, int howMany, int dimensions, int max, double[] center) {
        List<double[]> vectors = new ArrayList<double[]>();
        for (int i = 0; i < howMany; i++) {
            double[] vector = getRandomVector(dimensions, max, center);
            vectors.add(vector);
            tree.add(vector);
        }
        return vectors;
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
