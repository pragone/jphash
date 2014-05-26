package com.pragone.jphash.index;

public class Query {
    public final Vector q;
    public Vector bm;
    public double angularDistanceToBM;
    private QueryStats stats;

    public Query(Vector q) {
        this.q = q;
        this.reset();
    }

    public void increaseVisitedNodes() {
        if (stats != null) {
            stats.visitedNodes++;
        }
    }

    public void reset() {
        this.bm=null;
        this.angularDistanceToBM=Double.MAX_VALUE;
    }

    public void incrementEvaluatedVectors(int numVectors) {
        if (stats != null) {
            stats.evaluatedVectors += numVectors;
        }
    }

    public void setStats(QueryStats stats) {
        this.stats = stats;
    }
}