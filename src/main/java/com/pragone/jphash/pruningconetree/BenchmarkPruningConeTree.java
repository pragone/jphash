package com.pragone.jphash.pruningconetree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 14/05/2014
 * Time: 8:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class BenchmarkPruningConeTree {
    public static void main(String[] args) {
        int dimensions = 40;
        PruningConeTree tree = new PruningConeTree();
        int totalNodes = 100000;
        List<double[]> vectors = new ArrayList<double[]>(totalNodes);
        for (int i = 0; i < 10; i++) {
            vectors.addAll(populateRandom(null, (int) totalNodes/10, dimensions, 100, getRandomVector(dimensions, 500)));
        }
        System.out.println("Random vectors generated, starting tree fill");
        long start = System.currentTimeMillis();
        for (double[] vector : vectors) {
            tree.add(vector);
        }
        long end = System.currentTimeMillis();
        System.out.println("Filled tree with " + totalNodes + " in " + (end-start) + " ms");

        int numQueries = 1000;
        long durationAcum = 0;
        List<PruningConeTree.Query> queries = new ArrayList<PruningConeTree.Query>(numQueries);
        for (int i = 0; i < numQueries; i++) {
            queries.add(new PruningConeTree.Query(new PruningConeTree.Vector(getRandomVector(dimensions, 100))));
        }

        System.out.println("Starting queries");
        for (PruningConeTree.Query query : queries) {
            start = System.nanoTime();
            double[] resp = tree.query(query);
            durationAcum += (System.nanoTime() - start);
        }
        System.out.println("Did " + numQueries + " searches in average " + (((double)durationAcum)/(numQueries*1000000)) + " milliseconds each (total: " + durationAcum/1000000 + " ms)");
    }
    private static List<double[]> populateRandom(PruningConeTree tree, int howMany, int dimensions, int max) {
        return populateRandom(tree, howMany, dimensions, max, getRandomVector(dimensions,max));
    }
    private static List<double[]> populateRandom(PruningConeTree tree, int howMany, int dimensions, int max, double[] center) {
        List<double[]> vectors = new ArrayList<double[]>();
        for (int i = 0; i < howMany; i++) {
            double[] vector = getRandomVector(dimensions, max, center);
            vectors.add(vector);
            if (tree != null) {
                tree.add(vector);
            }
        }
        return vectors;
    }


    private static double[] getRandomVector(int size, int max, double[] center) {
        double[] temp = new double[size];
        for(int i = 0;  i < size; i++) {
            temp[i] = max*Math.random() + (center != null ? center[i] : 0);
        }
        return temp;
    }

    private static double[] getRandomVector(int size, int max) {
        return getRandomVector(size,max,null);
    }
}
