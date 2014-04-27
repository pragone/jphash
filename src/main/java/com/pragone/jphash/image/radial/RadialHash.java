package com.pragone.jphash.image.radial;

import com.pragone.jphash.util.HexUtil;

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

    @Override
    public String toString() {
        return HexUtil.byteArrayToString(coefficients);
    }

    public static RadialHash fromString(String string) {
        RadialHash temp = new RadialHash(string.length()/2);
        HexUtil.stringToByteArray(string, temp.coefficients);
        return temp;
    }
}
