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

    private final int maxNodeSize;
    protected List<double[]> vectors;
    protected IPTree left = null;
    protected IPTree right = null;
    private double[] mean;
    private double radius;
    private ReadWriteLock modificationLock = new ReentrantReadWriteLock();

    public IPTree(int maxNodeSize) {
        this.maxNodeSize = maxNodeSize;
    }

    public IPTree(int maxNodeSize, List<double[]> vectors) {
        this(maxNodeSize);
        this.vectors = vectors;
        balance();
    }

    public double[] query(double[] q) {
        try {
            modificationLock.readLock().lock();
            if (isEmpty()) {
                return null;
            }
            Query query = new Query(q);
            treeSearch(query, this);
            return query.bm;
        } finally {
            modificationLock.readLock().unlock();
        }
    }

    public void add(double[] vector) {
        try {
            modificationLock.writeLock().lock();
            if (vectors == null) {
                initVectors();
            }
            vectors.add(vector);
            if (vectors.size() > maxNodeSize) {
                balance();
            }
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    public void rebalance() {
        try {
            modificationLock.writeLock().lock();
            if (isLeaf()) return; // Nothing to do if I'm just a leaf node
            IPTree temp = new IPTree(maxNodeSize, this.getVectorsRecursive());
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
        this.vectors = other.vectors;
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

    private void balance() {
        try {
            modificationLock.writeLock().lock();
            if (vectors.size()<= maxNodeSize) {
                return;
            }
            this.mean = calculateMean();
            double[] lpivot = findFarthest(this.mean);
            double[] rpivot = findFarthest(lpivot);
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
            this.left = new IPTree(maxNodeSize, lvectors);
            this.right = new IPTree(maxNodeSize, rvectors);
            this.vectors = null;
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

    private double[] calculateMean() {
        if (vectors == null || vectors.isEmpty()) {
            return null;
        }
        int vectorSize = vectors.get(0).length;
        long[] acum = new long[vectorSize];
        for (double[] v : vectors) {
            for (int i = 0; i < vectorSize; i++) {
                acum[i] += v[i];
            }
        }
        double[] newMean = new double[vectorSize];
        int numVectors = vectors.size();
        for (int i = 0; i < vectorSize; i++) {
            newMean[i] = ((double) acum[i])/numVectors;
        }
        return newMean;
    }

    private double[] pickRandom() {
        return vectors.get((int) Math.rint(vectors.size()));
    }

    private synchronized void initVectors() {
        if (this.vectors == null) {
            this.vectors = new ArrayList<double[]>(maxNodeSize);
        }
    }

    private static void treeSearch(Query query, IPTree node) {
        if (query.lambda < mip(query, node)) {
            if (node.isLeaf()) {
                node.linearSearch(query);
            } else {
                double il = mip(query, node.left);
                double ir = mip(query, node.right);
                if (il < ir) {
                    treeSearch(query, node.left);
                } else {
                    treeSearch(query, node.right);
                }
            }
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

    private static int dotProduct(double[] v1, double[] v2) {
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

        public Query(double[] q) {
            this.q = q;
            this.bm = null;
            this.lambda = Integer.MIN_VALUE;
            this.qNorm = norm2(q);
        }

    }


}
