package com.pragone.jphash.index;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 14/05/2014
 * Time: 6:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class MergedSearchIndex implements SearchIndex {
    private final SearchIndexFactory factory;
    private List<SearchIndex> partitions = new ArrayList<SearchIndex>();

    public MergedSearchIndex(SearchIndexFactory factory) {
        this.factory = factory;
        this.partitions.add(factory.getNewSearchIndex());
    }

    @Override
    public Vector query(Query query) {
        for (SearchIndex partition : partitions) {
            partition.query(query);
        }
        return query.bm;
    }

    /**
     * Adds the vector to the last non readonly index
     * @param vector
     */
    @Override
    public boolean add(Vector vector) {
        for (int i = partitions.size() -1; i>=0; i--) {
            SearchIndex partition = partitions.get(i);
            if (!partition.isReadOnly()) {
                return partition.add(vector);
            }
        }
        throw new RuntimeException("All partitions of this index are read-only");
    }

    @Override
    public boolean isReadOnly() {
        for (int i = partitions.size() -1; i>=0; i--) {
            if (!partitions.get(i).isReadOnly()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void optimize() {
        // Let's optimize all partitions and add a new one if they all end up being readonly
        boolean needNewPartition = true;
        for (SearchIndex partition : partitions) {
            partition.optimize();
            if (!partition.isReadOnly()) {
                needNewPartition = false;
            }
        }
        if (needNewPartition) {
            partitions.add(factory.getNewSearchIndex());
        }
    }

    @Override
    public boolean optimizeMakesReadOnly() {
        return false;
    }
}
