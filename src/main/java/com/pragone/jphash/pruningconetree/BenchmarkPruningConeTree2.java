package com.pragone.jphash.pruningconetree;

import com.pragone.jphash.index.DoubleVector;
import com.pragone.jphash.index.Query;
import com.pragone.jphash.index.QueryStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 14/05/2014
 * Time: 8:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class BenchmarkPruningConeTree2 {
    public static void main(String[] args) {
        int dimensions = 40;
        PruningConeTree2 tree = new PruningConeTree2(null, Math.PI/2, 0.75, 15,15);
        int totalNodes = 100000;
        List<double[]> vectors = new ArrayList<double[]>(totalNodes);
        for (int i = 0; i < 10; i++) {
            vectors.addAll(populateRandom(null, (int) totalNodes/10, dimensions, 100, getRandomVector(dimensions, 500)));
        }
        System.out.println("NumVectors:\t" + totalNodes);
        System.out.println("DecreaseFactor:\t" + tree.getDecreaseFactor());
        System.out.println("MaxDepth: \t" + tree.getMaxDepth());
        System.out.println("MaxLeafSize: \t" + tree.getMaxLeafSize());
        System.out.println("Random vectors generated, starting tree fill");
        long start = System.currentTimeMillis();
        for (double[] vector : vectors) {
            tree.add(vector);
        }
        long end = System.currentTimeMillis();
        System.out.println("Filled tree with \t" + totalNodes + "\t in \t" + (end-start) + " ms");

        int numQueries = 2000;
        long durationAcum = 0;
        List<Query> queries = new ArrayList<Query>(numQueries);
        for (int i = 0; i < numQueries; i++) {
            queries.add(new Query(new DoubleVector(getRandomVector(dimensions, 100))));
        }

        System.out.println("Starting queries");
        QueryStats stats = new QueryStats();
        long acumEvaluatedVectors = 0;
        long acumVisitedNodes = 0;
        for (Query query : queries) {
            query.setStats(stats);
            start = System.nanoTime();
            tree.query(query);
            durationAcum += (System.nanoTime() - start);
            acumEvaluatedVectors += stats.evaluatedVectors;
            acumVisitedNodes += stats.visitedNodes;
            stats.reset();
        }
        System.out.println("Did " + numQueries + " searches in average \t" + (((double)durationAcum)/(numQueries*1000000)) + "\t milliseconds each (total: " + durationAcum/1000000 + " ms)");
        System.out.println("Average visited nodes: \t" + ((double) acumVisitedNodes)/numQueries + "\tAverage evaluated vectors: \t" + ((double) acumEvaluatedVectors)/numQueries);

        for (Query query : queries) {
            query.reset();
        }
        durationAcum = 0;
        System.out.println("Nodes before optimize: " + tree.getNumVectors());
        tree.optimize();
        System.out.println("Nodes after optimize: " + tree.getNumVectors());
        System.out.println("Starting queries (after optimize)");
        acumEvaluatedVectors = 0;
        acumVisitedNodes = 0;
        for (Query query : queries) {
            query.setStats(stats);
            start = System.nanoTime();
            tree.query(query);
            durationAcum += (System.nanoTime() - start);
            acumEvaluatedVectors += stats.evaluatedVectors;
            acumVisitedNodes += stats.visitedNodes;
            stats.reset();
        }
        System.out.println("Did " + numQueries + " searches in average \t" + (((double)durationAcum)/(numQueries*1000000)) + "\t milliseconds each (total: " + durationAcum/1000000 + " ms)");
        System.out.println("Average visited nodes: \t" + ((double) acumVisitedNodes)/numQueries + "\tAverage evaluated vectors: \t" + ((double) acumEvaluatedVectors)/numQueries);
    }
    private static List<double[]> populateRandom(PruningConeTree2 tree, int howMany, int dimensions, int max) {
        return populateRandom(tree, howMany, dimensions, max, getRandomVector(dimensions,max));
    }
    private static List<double[]> populateRandom(PruningConeTree2 tree, int howMany, int dimensions, int max, double[] center) {
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
