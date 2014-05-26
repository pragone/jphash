package com.pragone.jphash.index;

public class QueryStats {
    public int visitedNodes;
    public int evaluatedVectors;

    public void reset() {
        this.visitedNodes = 0;
        this.evaluatedVectors =0;
    }
}