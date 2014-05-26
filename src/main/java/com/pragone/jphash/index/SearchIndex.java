package com.pragone.jphash.index;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 14/05/2014
 * Time: 6:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SearchIndex {
    public Vector query(Query query);
    public boolean add(Vector vector);
    public boolean isReadOnly();
    public void optimize();
    public boolean optimizeMakesReadOnly();
}
