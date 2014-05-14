package com.pragone.jphash.pruningconetree;

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
        return query(new Query(new Vector(q)));
    }

    @Override
    public Vector query(Query query) {
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
            query.nodeExit();
        }
    }

    public void add(double[] vector) {
        add(new Vector(vector));
    }

    @Override
    public void add(Vector vector) {
        try {
            modificationLock.writeLock().lock();
            if (maxDepth == 0) {
                // can't spawn children. just add it here
                this.pivots.put(vector, EMPTY);
                return;
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

            int[] pivotsPerLevel = new int[maxDepth+1];
            countNonLeavePivotsPerLevel(this, pivotsPerLevel, 0);
            int[] leavesPerLevel = new int[maxDepth+1];
            countLeavesPerLevel(this, leavesPerLevel, 0);
            int[] maxPerLevel = new int[maxDepth+1];
            maxPivotsPerLevel(this, maxPerLevel, 0);
            System.out.println("Non-leaves per level\t" + tabJoin(pivotsPerLevel));
            System.out.println("Leaves per level\t" + tabJoin(leavesPerLevel));
            System.out.println("Max pivots per level\t" + tabJoin(maxPerLevel));

            removeEmptyNodes();

            pivotsPerLevel = new int[maxDepth+1];
            countNonLeavePivotsPerLevel(this, pivotsPerLevel, 0);
            leavesPerLevel = new int[maxDepth+1];
            countLeavesPerLevel(this, leavesPerLevel, 0);
            maxPerLevel = new int[maxDepth+1];
            maxPivotsPerLevel(this, maxPerLevel, 0);
            System.out.println("Non-leaves per level\t" + tabJoin(pivotsPerLevel));
            System.out.println("Leaves per level\t" + tabJoin(leavesPerLevel));
            System.out.println("Max pivots per level\t" + tabJoin(maxPerLevel));

        } finally {
            modificationLock.writeLock().unlock();
        }
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

    private void maxPivotsPerLevel(PruningConeTree tree, int[] pivotsPerLevel, int level) {
        if (pivotsPerLevel[level] < tree.pivots.size()) {
            pivotsPerLevel[level] = tree.pivots.size();
        }
        for (PruningConeTree pivot : tree.pivots.values()) {
            if (pivot != EMPTY) {
                maxPivotsPerLevel(pivot, pivotsPerLevel, level + 1);
            }
        }
    }
    private void countNonLeavePivotsPerLevel(PruningConeTree tree, int[] pivotsPerLevel, int level) {
        for (PruningConeTree pivot : tree.pivots.values()) {
            if (pivot != EMPTY) {
                pivotsPerLevel[level]++;
                countNonLeavePivotsPerLevel(pivot, pivotsPerLevel, level + 1);
            }
        }
    }
    private void countLeavesPerLevel(PruningConeTree tree, int[] pivotsPerLevel, int level) {
        for (PruningConeTree pivot : tree.pivots.values()) {
            if (pivot != EMPTY) {
                countLeavesPerLevel(pivot, pivotsPerLevel, level + 1);
            } else {
                pivotsPerLevel[level]++;
            }
        }
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
}
