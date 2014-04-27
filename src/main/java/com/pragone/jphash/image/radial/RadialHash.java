package com.pragone.jphash.image.radial;

/**
 * User: pragone
 * Date: 25/04/2014
 * Time: 5:21 PM
 */
public class RadialHash {
    private final byte[] coefficients;

    public RadialHash(int numberOfCoefficients) {
        this.coefficients = new byte[numberOfCoefficients];
    }

    public byte[] getCoefficients() {
        return coefficients;
    }
}
