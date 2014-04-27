package com.pragone.jphash;

import com.pragone.jphash.image.radial.RadialHash;
import com.pragone.jphash.image.radial.RadialHashAlgorithm;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: pragone
 * Date: 27/04/2014
 * Time: 5:34 PM
 */
public class jpHash {
    public RadialHash getImageRadialHash(String path) throws IOException {
        return RadialHashAlgorithm.getHash(path);
    }
    public RadialHash getImageRadialHash(File file) throws IOException {
        return RadialHashAlgorithm.getHash(file);
    }
    public RadialHash getImageRadialHash(InputStream is) throws IOException {
        return RadialHashAlgorithm.getHash(is);
    }
    public RadialHash getImageRadialHash(BufferedImage bi) throws IOException {
        return RadialHashAlgorithm.getHash(bi);
    }
    public double getSimilarity(RadialHash hash1, RadialHash hash2) {
        return RadialHashAlgorithm.getSimilarity(hash1, hash2);
    }
}
