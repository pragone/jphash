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

    @Test
    public void testAdd() {
        IPTree tree = new IPTree(100);
        tree.add(getRandomVector(40, 100));
    }

    @Test
    public void testNodeSplitAfterMaxNodeSize() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize);
        populateRandom(tree,nodeSize,40,100);
        Assert.assertTrue(tree.isLeaf());
        tree.add(getRandomVector(40,100));
        Assert.assertFalse(tree.isLeaf());
    }

    @Test
    public void testVectorArrayClearedOnSplit() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize);
        populateRandom(tree,nodeSize+1,40,100);
        Assert.assertNull(tree.vectors);
    }

    @Test
    public void testNoVectorLostOnSplit() {
        int nodeSize = 10;
        IPTree tree = new IPTree(nodeSize);
        populateRandom(tree,15,40,100);
        Assert.assertEquals(15, tree.left.getNumVectorsRecursive() + tree.right.getNumVectorsRecursive());
    }

    private List<double[]> populateRandom(IPTree tree, int howMany, int dimensions, int max) {
        List<double[]> vectors = new ArrayList<double[]>();
        for (int i = 0; i < howMany; i++) {
            double[] vector = getRandomVector(dimensions, max);
            vectors.add(vector);
            tree.add(vector);
        }
        return vectors;
    }

    private double[] getRandomVector(int size, int max) {
        double[] temp = new double[size];
        for(int i = 0;  i < size; i++) {
            temp[i] = max*Math.random();
        }
        return temp;
    }
}
