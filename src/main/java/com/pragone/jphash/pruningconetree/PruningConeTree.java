package com.pragone.jphash.pruningconetree;

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
public class PruningConeTree {
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
    protected Map<Vector, PruningConeTree> pivots = new HashMap<Vector, PruningConeTree>();
    private double decreaseFactor = DEFAULT_DECREASE_FACTOR;
    private final int maxDepth;


    public PruningConeTree(double maxAngularDistance, double decreaseFactor, int maxDepth) {
        this.maxAngularDistance = maxAngularDistance;
        this.decreaseFactor = decreaseFactor;
        this.maxDepth = maxDepth;
    }

    public PruningConeTree() {
        this(Math.PI/4, DEFAULT_DECREASE_FACTOR, DEFAULT_MAX_DEPTH);
    }

    public double[] query(double[] q) {
        return query(new Query(new Vector(q)));
    }

    public double[] query(Query query) {
        query.nodeEntry();
        try {
            modificationLock.readLock().lock();
            query.lockObtained();
            if (isEmpty()) {
                return null;
            }
            List<VectorDistance> pivotDistances = new ArrayList<VectorDistance>(this.pivots.size());
            // First pass
            for (Map.Entry<Vector, PruningConeTree> entry : this.pivots.entrySet()) {
                Vector pivot = entry.getKey();
                double pivotDistance = query.q.angularDistance(pivot);
                // There are no children under this pivot.
                if (pivotDistance < query.angularDistanceToBM) {
                    query.angularDistanceToBM = pivotDistance;
                    query.bm = pivot;
                }
                if (entry.getValue() != EMPTY) {
                    if (query.angularDistanceToBM > (pivotDistance-maxAngularDistance)) {
                        pivotDistances.add(new VectorDistance(pivot, pivotDistance));
                    }
                }
            }
            // Let's sort the pivots
            Collections.sort(pivotDistances, VECTOR_DISTANCE_SORTER);
            for (VectorDistance vectorDistance : pivotDistances) {
                if (query.angularDistanceToBM > (vectorDistance.value-maxAngularDistance)) {
                    // It may contain a closer vector
                    this.pivots.get(vectorDistance.key).query(query);
                }
            }
            return query.bm.coords;
        } finally {
            modificationLock.readLock().unlock();
            query.nodeExit();
        }
    }

    public void add(double[] vector) {
        add(new Vector(vector));
    }

    public void add(Vector vector) {
        try {
            modificationLock.writeLock().lock();
            if (maxDepth == 0) {
                // can't spawn children. just add it here
                this.pivots.put(vector, EMPTY);
                return;
            }

            for (Map.Entry<Vector, PruningConeTree> entry : this.pivots.entrySet()) {
                double distance = vector.angularDistance(entry.getKey());
                if (distance < maxAngularDistance) {
                    PruningConeTree branch = entry.getValue();
                    if (branch == EMPTY) {
                        branch = new PruningConeTree(maxAngularDistance*decreaseFactor,decreaseFactor,maxDepth-1);
                        branch.add(entry.getKey());
                        this.pivots.put(entry.getKey(), branch);
                    }
                    branch.add(vector);
                    return;
                }
            }
            this.pivots.put(vector, EMPTY);
        } finally {
            modificationLock.writeLock().unlock();
        }
    }

    private boolean isEmpty() {
        return this.pivots.isEmpty();
    }

    public static class Query {
        private final Vector q;
        private Vector bm;
        private double angularDistanceToBM;
        private QueryStats stats;

        public Query(Vector q) {
            this.q = q;
            this.bm = null;
            this.angularDistanceToBM = Double.MAX_VALUE;
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

        public void nodeEntry() {
            //To change body of created methods use File | Settings | File Templates.
        }

        public void nodeExit() {
            //To change body of created methods use File | Settings | File Templates.
        }

        public void lockObtained() {
            //To change body of created methods use File | Settings | File Templates.
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

    public static class Vector {
        private final double[] coords;
        private double norm2 = -1;

        public Vector(double[] coordinates) {
            this.coords = coordinates;
        }

        public double getNorm2() {
            if (norm2 < 0) {
                calculateNorm2();
            }
            return norm2;
        }

        protected void calculateNorm2() {
            double resp = 0;
            for (int i = 0; i < coords.length; i++) {
                resp += coords[i]*coords[i];
            }
            this.norm2 = Math.sqrt(resp);
        }

        public double similarity(Vector other) {
            if (other == null || other.getDimensions() != getDimensions()) {
                throw new IllegalArgumentException("Similarity can only be calculated on two non-null equal-size vectors");
            }
            double t = dotProduct(other);
            if (t <= 0) {
                return 0;
            } else {
                t = t / (getNorm2()*other.getNorm2());
                if (t > 1){
                    return 1;
                }
                return t;
            }
        }

        public double dotProduct(Vector other) {
            if (other == null || other.getDimensions() != getDimensions()) {
                throw new IllegalArgumentException("Dot Product can only be calculated on two non-null equal-size vectors");
            }
            int resp = 0;
            for (int i = 0; i < coords.length; i++) {
                resp += coords[i]*other.coords[i];
            }
            return resp;
        }

        public double angularDistance(Vector other) {
            if (other == null || other.getDimensions() != getDimensions()) {
                throw new IllegalArgumentException("Angular distance can only be calculated on two non-null equal-size vectors");
            }
            return Math.acos(similarity(other));
        }

        public int getDimensions() {
            return this.coords.length;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (double element : coords) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(element);
            }
            sb.append('}');
            return sb.toString();
        }
    }

    private static class VectorDistance {
        public Vector key;
        public double value;

        public VectorDistance(Vector key, double value) {
            this.key = key;
            this.value = value;
        }
    }
}
