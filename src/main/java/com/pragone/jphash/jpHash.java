package com.pragone.jphash;

import com.pragone.jphash.image.radial.RadialHash;
import com.pragone.jphash.image.radial.RadialHashAlgorithm;

import java.io.IOException;

/**
 * User: pragone
 * Date: 27/04/2014
 * Time: 5:34 PM
 */
public class jpHash {
    public RadialHash calculateRadialHash(String path) throws IOException {
        return RadialHashAlgorithm.getHash3(path);
    }
}
