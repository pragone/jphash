package com.pragone.jphash.index;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 27/05/2014
 * Time: 7:26 AM
 * To change this template use File | Settings | File Templates.
 */
public interface Vector {
    double getNorm2();

    double similarity(Vector other);

    double dotProduct(Vector other);

    double angularDistance(Vector other);

    int getDimensions();

    double getCoord(int index);

    void multiply(double scale);
}
