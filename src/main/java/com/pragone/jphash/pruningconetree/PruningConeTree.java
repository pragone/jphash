package com.pragone.jphash.pruningconetree;

import com.pragone.jphash.index.DoubleVector;
import com.pragone.jphash.index.Query;
import com.pragone.jphash.index.SearchIndex;
import com.pragone.jphash.index.Vector;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 30/04/2014
 * Time: 8:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class PruningConeTree implements SearchIndex {
    private static final double DEFAULT_DECREASE_FACTOR = 0.75;
    private static final int DEFAULT_MAX_DEPTH = 10;
    private static final Comparator<VectorDistance> VECTOR_DISTANCE_SORTER = new Comparator<VectorDistance>() {
        @Override
        public int compare(VectorDistance vd1, VectorDistance vd2) {
            return Double.compare(vd1.value, vd2.value);
        }
    };
    private ReadWriteLock modificationLock = new ReentrantReadWriteLock();
    private static final PruningConeTree EMPTY = new PruningConeTree(0,0,0);

    // Tree configuration info
    private double maxAngularDistance;

    // Info for leafs
    private PruningConeTree parent = null;
    protected Map<Vector, PruningConeTree> pivots = new HashMap<Vector, PruningConeTree>();
    private double decreaseFactor = DEFAULT_DECREASE_FACTOR;
    private int maxDepth;
    private boolean readOnly =false;


    public PruningConeTree(double maxAngularDistance, double decreaseFactor, int maxDepth) {
        this.maxAngularDistance = maxAngularDistance;
        this.decreaseFactor = decreaseFactor;
        this.maxDepth = maxDepth;
    }

    public PruningConeTree() {
        this(Math.PI/4, DEFAULT_DECREASE_FACTOR, DEFAULT_MAX_DEPTH);
    }

    public Vector query(double[] q) {
        return query(new Query(new DoubleVector(q)));
    }

    @Override
    public Vector query(Query query) {
        query.increaseVisitedNodes();
        try {
            modificationLock.readLock().lock();
            if (isEmpty()) {
                return null;
            }
            List<VectorDistance> pivotDistances = new ArrayList<VectorDistance>(this.pivots.size());
            // First pass
            for (Map.Entry<Vector, PruningConeTree> entry : this.pivots.entrySet()) {
                Vector pivot = entry.getKey();
                PruningConeTree branch = entry.getValue();
                double pivotDistance = query.q.angularDistance(pivot);
                // There are no children under this pivot.
                if (pivotDistance < query.angularDistanceToBM) {
                    query.angularDistanceToBM = pivotDistance;
                    query.bm = pivot;
                }
                if (branch != EMPTY) {
                    if (query.angularDistanceToBM > (pivotDistance-branch.maxAngularDistance)) {
                        pivotDistances.add(new VectorDistance(pivot, pivotDistance, branch.maxAngularDistance));
                    }
                }
            }
            // Let's sort the pivots
            Collections.sort(pivotDistances, VECTOR_DISTANCE_SORTER);
            for (VectorDistance vectorDistance : pivotDistances) {
                if (query.angularDistanceToBM > (vectorDistance.value-vectorDistance.branchMaxAngularDistance)) {
                    // It may contain a closer vector
                    this.pivots.get(vectorDistance.key).query(query);
                }
            }
            return query.bm;
        } finally {
            modificationLock.readLock().unlock();
        }
    }

    public void add(double[] vector) {
        add(new DoubleVector(vector));
    }

    @Override
    public boolean add(Vector vector) {
        try {
            modificationLock.writeLock().lock();
            if (maxDepth == 0) {
                // can't spawn children. just add it here
                this.pivots.put(vector, EMPTY);
                return true;
            }

            for (Map.Entry<Vector, PruningConeTree> entry : this.pivots.entrySet()) {
                Vector pivot = entry.getKey();
                PruningConeTree branch = entry.getValue();
                double distance = vector.angularDistance(pivot);
                double pivotDistanceThreshold = this.maxAngularDistance;
                if (branch != EMPTY) {
                    pivotDistanceThreshold = branch.maxAngularDistance;
                }
                if (distance < pivotDistanceThreshold) {
                    if (branch == EMPTY) {
                        branch = new PruningConeTree(this.maxAngularDistance*decreaseFactor,decreaseFactor,maxDepth-1);
                        branch.add(entry.getKey());
                        branch.parent = this;
                        this.pivots.put(entry.getKey(), branch);
                    }
                    branch.add(vector);
                    return true;
                }
            }
            this.pivots.put(vector, EMPTY);
            return true;
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isReadOnly() {
        try {
            modificationLock.readLock().lock();
            return readOnly;
        } finally {
            modificationLock.readLock().unlock();
        }
    }

    @Override
    public void optimize() {
        try {
            modificationLock.writeLock().lock();
            this.readOnly = true;

            printNodeStats();
            System.out.println("Removing empty nodes");
            long start = System.currentTimeMillis();
            removeEmptyNodes();
            System.out.println("Done removing empty nodes in " + (System.currentTimeMillis() - start) + " ms");
            printNodeStats();
            splitNodesBiggerThan(40);

        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    private void splitNodesBiggerThan(int maxNodeSize) {
        if (getNumNonLeafNodes() > maxNodeSize) {
            doNodeSplit(maxNodeSize);
        }
        for (PruningConeTree pivot : pivots.values()) {
            if (pivot != EMPTY) {
                pivot.splitNodesBiggerThan(maxNodeSize);
            }
        }
    }

    private int getNumNonLeafNodes() {
        int nonLeafNodes =0;
        for (PruningConeTree pivot : pivots.values()) {
            if (pivot != EMPTY) {
                nonLeafNodes++;
            }
        }
        return nonLeafNodes;
    }

    private void doNodeSplit(int maxNodeSize) {
        int maxIterations = 4;
        double maxAngularDistance = this.maxAngularDistance;
        int nonLeafNodes = getNumNonLeafNodes();
        while (maxIterations > 0 && nonLeafNodes > maxNodeSize) {

            maxAngularDistance = this.maxAngularDistance * (1-(1-this.decreaseFactor) * (((double) nonLeafNodes - maxNodeSize)/nonLeafNodes)/2);
            nonLeafNodes = simulateAngularDistance(maxAngularDistance);
            maxIterations--;
        }
        rebalanceWithAngularDistance(maxAngularDistance);
    }

    private void rebalanceWithAngularDistance(double maxAngularDistance) {
        PruningConeTree tempTree = new PruningConeTree(this.maxAngularDistance, maxAngularDistance/this.maxAngularDistance, maxDepth);
        copyLeafsTo(tempTree);
        this.maxAngularDistance = maxAngularDistance;
        this.pivots = tempTree.pivots;
    }

    private void copyLeafsTo(PruningConeTree tempTree) {
        for (Map.Entry<Vector, PruningConeTree> entry : this.pivots.entrySet()) {
            PruningConeTree subTree = entry.getValue();
            if (subTree == EMPTY) {
                tempTree.add(entry.getKey());
            } else {
                subTree.copyLeafsTo(tempTree);
            }
        }
    }

    private int simulateAngularDistance(double maxAngularDistance) {
        List<Vector> newPivots = new ArrayList<Vector>(this.pivots.size());
        for (Map.Entry<Vector, PruningConeTree> entry : pivots.entrySet()) {
            Vector pivot = entry.getKey();
            if (entry.getValue() != EMPTY) {
                // It's a non-leaf pivot
                boolean joined = false;
                for (Vector newPivot : newPivots) {
                    if (newPivot.angularDistance(pivot) < maxAngularDistance) {
                        joined = true;
                        break;
                    }
                }
                if (!joined) {
                    newPivots.add(pivot);
                }
            }
        }
        return newPivots.size();
    }

    private void printNodeStats() {
        StatsCollector statsCollector = new StatsCollector();
        collectStats(statsCollector, 0);

        System.out.println("Leaf pivots per level\t" + tabJoin(statsCollector.leafNodes));
        System.out.println("Max non-leaf pivots per level\t" + tabJoin(statsCollector.maxPivots));
        System.out.println("Acum non-leaf pivots per level\t" + tabJoin(statsCollector.acum));
        System.out.println("Num nodes per level\t" + tabJoin(statsCollector.count));
        System.out.println("Average non-leaf pivots per node\t" + tabJoin(statsCollector.getAveragePivotsPerNode()));
        System.out.println("StdDev non-leaf pivots per node\t" + tabJoin(statsCollector.getStdDevPivotsPerNode()));
    }

    private void collectStats(StatsCollector stats, int level) {
        stats.count[level]++;
        if (stats.maxPivots[level] < pivots.size()) {
            stats.maxPivots[level] = pivots.size();
        }
        int nonLeafNodes =0;
        for (PruningConeTree pivot : pivots.values()) {
            if (pivot != EMPTY) {
                nonLeafNodes++;
                pivot.collectStats(stats, level + 1);
            } else {
                stats.leafNodes[level]++;
            }
        }
        stats.nonLeafNodes[level] += nonLeafNodes;
        stats.acum[level] += nonLeafNodes;
        stats.acumSq[level] += nonLeafNodes*nonLeafNodes;
    }


    private void removeEmptyNodes() {
        while (this.pivots.size() == 1) {
            PruningConeTree child = this.pivots.values().iterator().next();
            if (child != EMPTY) {
                this.maxAngularDistance = child.maxAngularDistance;
                this.pivots = child.pivots;
                this.maxDepth = child.maxDepth;
            } else {
                // This one child is a leaf, so nothing to do
                break;
            }
        }
        for (PruningConeTree child : this.pivots.values()) {
            if (child != EMPTY) {
                child.removeEmptyNodes();
            }
        }
    }

    private String tabJoin(int[] vals) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int val : vals) {
            if (!first) {
                sb.append('\t');
            } else {
                first = false;
            }
            sb.append(val);
        }
        return sb.toString();
    }

    private String tabJoin(double[] vals) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (double val : vals) {
            if (!first) {
                sb.append('\t');
            } else {
                first = false;
            }
            sb.append(val);
        }
        return sb.toString();
    }

    @Override
    public boolean optimizeMakesReadOnly() {
        return true;
    }

    private boolean isEmpty() {
        return this.pivots.isEmpty();
    }

    private static class VectorDistance {
        public Vector key;
        public double value;
        public double branchMaxAngularDistance;

        public VectorDistance(Vector key, double value, double branchMaxAngularDistance) {
            this.key = key;
            this.value = value;
            this.branchMaxAngularDistance = branchMaxAngularDistance;
        }
    }

    private class StatsCollector {
        public int[] acum = new int[maxDepth+1];
        public int[] count = new int[maxDepth+1];
        public int[] maxPivots = new int[maxDepth+1];
        public int[] nonLeafNodes = new int[maxDepth+1];
        public int[] leafNodes = new int[maxDepth+1];
        public int[] acumSq = new int[maxDepth+1];

        public double[] getAveragePivotsPerNode() {
            double[] temp = new double[maxDepth+1];
            for (int i = 0; i <= maxDepth; i++) {
                temp[i] = ((double) acum[i])/count[i];
            }
            return temp;
        }
        public double[] getStdDevPivotsPerNode() {
            double[] temp = new double[maxDepth+1];
            for (int i = 0; i <= maxDepth; i++) {
                temp[i] = Math.sqrt(acumSq[i] - Math.pow(((double) acum[i])/count[i],2));
            }
            return temp;
        }
    }
}
