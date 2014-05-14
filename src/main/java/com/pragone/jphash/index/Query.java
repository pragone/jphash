package com.pragone.jphash.index;

public class Query {
    public final Vector q;
    public Vector bm;
    public double angularDistanceToBM;
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

    public void reset() {
        this.bm=null;
        this.angularDistanceToBM=Double.MAX_VALUE;
    }
}