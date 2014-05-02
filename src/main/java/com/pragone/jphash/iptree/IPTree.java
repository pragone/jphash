package com.pragone.jphash.iptree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 30/04/2014
 * Time: 8:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class IPTree {
    private ReadWriteLock modificationLock = new ReentrantReadWriteLock();

    // Tree configuration info
    private final int maxNodeSize;
    private final int dimensions;

    // Info for leafs
    protected List<double[]> vectors;

    // Info for non-leaf
    protected double[] lpivot = null;
    protected double[] rpivot = null;
    protected IPTree left = null;
    protected IPTree right = null;

    // Content info... this information needs to be updated on each add
    private double[] mean;
    private double radius = 0;
    private long numVectors = 0;
    protected double[] meanAcum;

    public IPTree(int maxNodeSize, int dimensions) {
        this.maxNodeSize = maxNodeSize;
        this.dimensions = dimensions;
        this.meanAcum = new double[dimensions];
        this.mean = new double[dimensions];
        this.vectors = new ArrayList<double[]>(maxNodeSize);
    }

    public IPTree(int maxNodeSize, int dimensions, List<double[]> vectors) {
        this(maxNodeSize, dimensions);
        this.vectors.addAll(vectors);
        updateContentInfo();
        splitIfNecessary();
    }

    public double[] query(double[] q) {
        return query(q,null);
    }

    public double[] query(double[] q, QueryStats stats) {
        try {
            modificationLock.readLock().lock();
            if (isEmpty()) {
                return null;
            }
            Query query = new Query(q);
            query.stats = stats;
            query.setStartTime();
            treeSearch(query, this);
            query.setEndTime();
            return query.bm;
        } finally {
            modificationLock.readLock().unlock();
        }
    }

    public void add(double[] vector) {
        if (!isLeaf()) {
            double ldist = euclideanDistanceSquared(vector, lpivot);
            double rdist = euclideanDistanceSquared(vector, rpivot);
            if (ldist < rdist) {
                left.add(vector);
            } else {
                right.add(vector);
            }
        } else {
            try {
                modificationLock.writeLock().lock();
                vectors.add(vector);
                if (vectors.size() > maxNodeSize) {
                    splitIfNecessary();
                }
            } finally {
                modificationLock.writeLock().unlock();
            }
        }
    }

    public void rebalance() {
        try {
            modificationLock.writeLock().lock();
            if (isLeaf()) return; // Nothing to do if I'm just a leaf node
            IPTree temp = new IPTree(maxNodeSize, dimensions, this.getVectorsRecursive());
            copy(temp);
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    private void copy(IPTree other) {
        this.left = other.left;
        this.right = other.right;
        this.radius = other.radius;
        this.mean = other.mean;
        this.meanAcum = other.meanAcum;
        this.numVectors = other.numVectors;
        this.vectors = other.vectors;
        this.lpivot = other.lpivot;
        this.rpivot = other.rpivot;
    }

    public List<double[]> getVectorsRecursive() {
        int fullSize = getNumVectorsRecursive();
        List<double[]> resp = new ArrayList<double[]>(fullSize);
        addVectorsRecursive(this,resp);
        return resp;
    }

    private static void addVectorsRecursive(IPTree ipTree, List<double[]> resp) {
        if (ipTree.isLeaf()) {
            resp.addAll(ipTree.vectors);
        } else {
            addVectorsRecursive(ipTree.left, resp);
            addVectorsRecursive(ipTree.right, resp);
        }
    }

    public int getNumVectorsRecursive() {
        if (isLeaf()) {
            return vectors == null ? 0 : vectors.size();
        }
        return left.getNumVectorsRecursive() + right.getNumVectorsRecursive();
    }

    private void splitIfNecessary() {
        try {
            modificationLock.writeLock().lock();
            if (vectors.size()<= maxNodeSize) {
                return;
            }
            this.lpivot = findFarthest(this.mean);
            this.rpivot = findFarthest(lpivot);
            this.radius = euclideanDistanceSquared(this.mean, lpivot);
            ArrayList<double[]> lvectors = new ArrayList<double[]>(vectors.size());
            ArrayList<double[]> rvectors = new ArrayList<double[]>(vectors.size());
            for (double[] v : vectors) {
                double ldist = euclideanDistanceSquared(v, lpivot);
                double rdist = euclideanDistanceSquared(v, rpivot);
                if (ldist < rdist) {
                    lvectors.add(v);
                } else {
                    rvectors.add(v);
                }
            }
            this.vectors = null;
            this.left = new IPTree(maxNodeSize, dimensions, lvectors);
            lvectors = null;
            this.right = new IPTree(maxNodeSize, dimensions, rvectors);
            rvectors = null;
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    private double[] findFarthest(double[] vector) {
        double maxDistance = -1;
        double[] farthest = null;
        for (double[] v : vectors) {
            double d = euclideanDistanceSquared(vector, v);
            if (d > maxDistance) {
                maxDistance = d;
                farthest = v;
            }
        }
        return farthest;
    }

    /**
     * Updates all the fields that have to do with the content of this tree.
     * Namely: mean, radius, numVectors and meanAcum;
     *
     * You can only invoke this method on a leaf node
     * @throws IllegalStateException If this tree represents a non-leaf node
     */
    private void updateContentInfo() {
        if (!isLeaf()) {
            throw new IllegalStateException("Can't call updateContentInfo on a non-leaf node");
        }
        if (vectors == null || vectors.isEmpty()) {
            return;
        }
        this.numVectors = this.vectors.size();
        this.meanAcum = new double[dimensions];
        for (double[] v : vectors) {
            for (int i = 0; i < dimensions; i++) {
                this.meanAcum[i] += v[i];
            }
        }
        this.mean = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            this.mean[i] = ((double) this.meanAcum[i])/ numVectors;
        }
        this.radius = 0;
        for (double[] v : vectors) {
            double temp = euclideanDistanceSquared(this.mean, v);
            if (temp > this.radius) {
                this.radius = temp;
            }
        }
    }

    private static void treeSearch(Query query, IPTree node) {
        if (query.lambda < mip(query, node)) {
            query.markVisited();
            if (node.isLeaf()) {
                node.linearSearch(query);
                query.countVisitedVectors(node.getNumVectorsRecursive());
            } else {
                double il = mip(query, node.left);
                double ir = mip(query, node.right);
                if (il < ir) {
                    treeSearch(query, node.right);
                    treeSearch(query, node.left);
                } else {
                    treeSearch(query, node.left);
                    treeSearch(query, node.right);
                }
            }
        } else {
            query.markSkipped();
            query.countSkippedVectors(node.getNumVectorsRecursive());
        }
    }

    private static double mip(Query query, IPTree node) {
        //MIP(q, T ) = <q, T.µ> + ||q||*T.R
        return dotProduct(query.q, node.mean) + query.qNorm*node.radius;
    }


    protected boolean isLeaf() {
        return this.left == null;
    }

    private void linearSearch(Query query) {
        if (vectors == null) return;
        for (double[] v : vectors) {
            int dp = dotProduct(query.q, v);
            if (dp > query.lambda) {
                query.bm = v;
                query.lambda = dp;
            }
        }
    }

    public static int dotProduct(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            throw new IllegalArgumentException("Dot Product can only be calculated on two non-null equal-size vectors");
        }
        int resp = 0;
        for (int i = 0; i < v1.length; i++) {
            resp += v1[i]*v2[i];
        }
        return resp;
    }

    private double euclideanDistanceSquared(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            throw new IllegalArgumentException("Euclidean distance can only be calculated on two non-null equal-size vectors");
        }
        double resp = 0;
        for (int i = 0; i < v1.length; i++) {
            double t = v1[i] - v2[i];
            resp += t*t;
        }
        return resp;
    }

    private static double norm2(double[] q) {
        if (q == null) {
            throw new IllegalArgumentException("Norm can only be calculated on a non-null vector");
        }
        double resp = 0;
        for (int i = 0; i < q.length; i++) {
            resp += q[i]*q[i];
        }
        return Math.sqrt(resp);
    }

    private boolean isEmpty() {
        return isLeaf() ? (this.vectors == null || this.vectors.isEmpty()) : false;
    }

    private static class Query {
        private final double[] q;
        private final double qNorm;
        private double[] bm;
        private int lambda;
        private QueryStats stats;

        public Query(double[] q) {
            this.q = q;
            this.bm = null;
            this.lambda = Integer.MIN_VALUE;
            this.qNorm = Math.sqrt(norm2(q));
        }

        private void markVisited() {
            if (stats != null) {
                stats.visitedNodes++;
            }
        }
        private void markSkipped() {
            if (stats != null) {
                stats.skippedNodes++;
            }
        }

        public void setStartTime() {
            if (stats != null) {
                stats.startTime = System.currentTimeMillis();
            }
        }

        public void setEndTime() {
            if (stats != null) {
                stats.duration = System.currentTimeMillis() - stats.startTime;
            }
        }

        public void countVisitedVectors(int numVectors) {
            if (stats != null) {
                stats.visitedVectors += numVectors;
            }
        }

        public void countSkippedVectors(int numVectors) {
            if (stats != null) {
                stats.skippedVectors += numVectors;
            }
        }
    }

    public static class QueryStats {
        public int visitedNodes;
        public int skippedNodes;
        public long startTime;
        public long duration;
        public int visitedVectors;
        public int skippedVectors;
    }
}
