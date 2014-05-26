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
public class PruningConeTree2 implements SearchIndex {
    public static final double DEFAULT_DECREASE_FACTOR = 0.75;
    public static final int DEFAULT_MAX_DEPTH = 10;
    private static final Comparator<VectorDistance> VECTOR_DISTANCE_SORTER = new Comparator<VectorDistance>() {
        @Override
        public int compare(VectorDistance vd1, VectorDistance vd2) {
            return Double.compare(vd1.value, vd2.value);
        }
    };
    public static final int DEFAULT_MAX_LEAF_SIZE = 20;
    private ReadWriteLock modificationLock = new ReentrantReadWriteLock();

    // Tree configuration info (all vectors of this tree must be within this angular distance of the pivot)
    private Vector pivot;
    private double maxAngularDistance;
    private int maxLeafSize; // How many vectors can this tree contain before it needs to split
    private double decreaseFactor = DEFAULT_DECREASE_FACTOR; // The decrease step for children trees
    private int maxDepth; // How much depth below this node is allowed. (0 means that this node must be a leaf)

    // State for leaf nodes
    private List<Vector> leafVectors;

    // State for non-leaf nodes
    private List<PruningConeTree2> children;
    private boolean readOnly = false;

    // Optimizations
    private double actualMaxAngularDistance = 0;

    public PruningConeTree2() {
        this(null, Math.PI/2, DEFAULT_DECREASE_FACTOR, DEFAULT_MAX_DEPTH, DEFAULT_MAX_LEAF_SIZE);
    }

    private static Vector getCenter(int numDimensions) {
        double[] coords = new double[numDimensions];
        for (int i = 0; i < numDimensions; i++) {
            coords[i] = 10;
        }
        return new DoubleVector(coords);
    }

    public PruningConeTree2(Vector pivot, double maxAngularDistance, double decreaseFactor, int maxDepth, int maxLeafSize) {
        this.pivot = pivot;
        this.maxAngularDistance = maxAngularDistance;
        this.decreaseFactor = decreaseFactor;
        this.maxDepth = maxDepth;
        this.leafVectors = new ArrayList<Vector>();
        this.maxLeafSize = maxLeafSize;
        if (pivot != null) {
            this.leafVectors.add(pivot);
        }
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
            double angularDistanceToPivot = 0;
            if (pivot != null) {
                query.incrementEvaluatedVectors(1);
                angularDistanceToPivot = pivot.angularDistance(query.q);
                if (angularDistanceToPivot > (query.angularDistanceToBM + this.actualMaxAngularDistance)) {
                    // We can't have a closer vector -- prune the search
                    return query.bm;
                }
            }
            // We haven't split yet
            if (leafVectors != null) {
                query.incrementEvaluatedVectors(this.leafVectors.size());
                for (Vector vector : this.leafVectors) {
                    double ad = query.q.angularDistance(vector);
                    if (ad < query.angularDistanceToBM) {
                        query.bm=vector;
                        query.angularDistanceToBM=ad;
                    }
                }
            } else {
                if (this.pivot != null && (angularDistanceToPivot < query.angularDistanceToBM)) {
                    query.angularDistanceToBM = angularDistanceToPivot;
                    query.bm = pivot;
                }
                for (PruningConeTree2 branch : this.children) {
                    branch.query(query);
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
            if (readOnly) {
                throw new IllegalStateException("Index is readonly");
            }
            modificationLock.writeLock().lock();
            if (pivot != null) {
                double angularDistanceToPivot = pivot.angularDistance(vector);
                if (angularDistanceToPivot >= this.maxAngularDistance) {
                    // This vector doesn't belong here...
                    return false;
                }
                if (angularDistanceToPivot > actualMaxAngularDistance) {
                    actualMaxAngularDistance = angularDistanceToPivot;
                }
            }
            if (this.leafVectors != null && (this.leafVectors.size() < maxLeafSize || maxDepth == 0)) {
                // can't spawn children. just add it here
                this.leafVectors.add(vector);
                return true;
            }
            //Should go to child
            if (this.children == null) {
                doSplit();
            }
            for (PruningConeTree2 branch : this.children) {
                if (branch.add(vector)) {
                    return true;
                }
            }
            // No branch has it, so we need to create  new child
            this.children.add(new PruningConeTree2(vector, this.maxAngularDistance * this.decreaseFactor, this.decreaseFactor, this.maxDepth-1, maxLeafSize));
            return true;
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    private void doSplit() {
        // Naive implementation
        List<Vector> leaves = this.leafVectors;
        this.leafVectors = null;
        this.children = new ArrayList<PruningConeTree2>();
        for (Vector vector : leaves) {
            add(vector);
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
            start = System.currentTimeMillis();
            splitIfBiggerThan(40);
            System.out.println("Done splitting big nodes in " + (System.currentTimeMillis() - start) + " ms");
            printNodeStats();
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    private void splitIfBiggerThan(int maxNodeSize) {
        if (this.children == null) {
            return;
        }
        if (this.children.size() > maxNodeSize) {
            int maxIterations = 4;
            double maxAngularDistance = this.maxAngularDistance;
            int nonLeafNodes = this.children.size();
            while (maxIterations > 0 && nonLeafNodes > maxNodeSize) {
                maxAngularDistance = this.maxAngularDistance * (1-(1-this.decreaseFactor) * (((double) nonLeafNodes - maxNodeSize)/nonLeafNodes)/2);
                nonLeafNodes = simulateAngularDistance(maxAngularDistance);
                maxIterations--;
            }
            rebalanceWithAngularDistance(maxAngularDistance);
        }
        for (PruningConeTree2 child : children) {
            child.splitIfBiggerThan(maxNodeSize);
        }
    }

    private void rebalanceWithAngularDistance(double maxAngularDistance) {
        PruningConeTree2 tempTree = new PruningConeTree2(null, this.maxAngularDistance, maxAngularDistance/this.maxAngularDistance, maxDepth, maxLeafSize);
        copyLeafsTo(tempTree);
        this.maxAngularDistance = maxAngularDistance;
        this.children = tempTree.children;
        this.actualMaxAngularDistance = tempTree.actualMaxAngularDistance;
    }

    private void copyLeafsTo(PruningConeTree2 tempTree) {
        if (this.leafVectors != null) {
            tempTree.addAll(this.leafVectors);
        } else {
            for (PruningConeTree2 child : this.children) {
                child.copyLeafsTo(tempTree);
            }
        }
    }

    private void addAll(List<Vector> vectors) {
        for (Vector vector : vectors) {
            this.add(vector);
        }
    }

    public long getNumVectors() {
        if (this.leafVectors != null) {
            return this.leafVectors.size();
        } else {
            long acum = 0;
            for (PruningConeTree2 child : children) {
                acum += child.getNumVectors();
            }
            return acum;
        }
    }

    private int simulateAngularDistance(double maxAngularDistance) {
        List<Vector> newPivots = new ArrayList<Vector>(this.children.size());
        for (PruningConeTree2 child : children) {
            Vector pivot = child.pivot;
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
        return newPivots.size();
    }

    private void printNodeStats() {
        StatsCollector statsCollector = new StatsCollector();
        collectStats(statsCollector, 0);

        System.out.println("Num nodes\t" + tabJoin(statsCollector.numNodes));
        System.out.println("Num split nodes\t" + tabJoin(statsCollector.numSplittedNodes));
        System.out.println("Max children per node\t" + tabJoin(statsCollector.maxChildrenPerNode));

    }

    private void collectStats(StatsCollector stats, int level) {
        stats.numNodes[level]++;
        if (this.children != null && stats.maxChildrenPerNode[level] < children.size()) {
            stats.maxChildrenPerNode[level] = children.size();
        }
        if (this.leafVectors != null) {
            stats.leafNodes[level] += this.leafVectors.size();
        } else {
            stats.numSplittedNodes[level]++;
            for (PruningConeTree2 child : children) {
                child.collectStats(stats, level + 1);
            }
        }
    }


    private void removeEmptyNodes() {
        if (this.children == null) {
            return;
        }
        while (this.children.size() == 1) {
            PruningConeTree2 child = this.children.get(0);
            this.maxAngularDistance = child.maxAngularDistance;
            this.actualMaxAngularDistance = child.actualMaxAngularDistance;
            this.children = child.children;
            this.pivot = child.pivot;
        }
        for (PruningConeTree2 child : this.children) {
            child.removeEmptyNodes();
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
        return (this.leafVectors != null && this.leafVectors.isEmpty()) ||
                (this.children != null && this.children.isEmpty());
    }

    public double getDecreaseFactor() {
        return decreaseFactor;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxLeafSize() {
        return maxLeafSize;
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
        public int[] numNodes = new int[maxDepth+1];
        public int[] numSplittedNodes = new int[maxDepth+1];

        public int[] maxChildrenPerNode = new int[maxDepth+1];
        public int[] leafNodes = new int[maxDepth+1];
    }
}
