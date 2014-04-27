package com.pragone.jphash.image.radial;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 27/04/2014
 * Time: 6:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class RadialHashAlgorithmTest {

    private static final String EARTH1_HASH = "71fe3e4b002fb995ab6d466e8e7c7061705e6f7784727c7f897b7a6970757d6e6b646d67696a6768";
    private static final String EARTH2_HASH = "9dff0c5e0039a797e28bc39f939bb8b1a798aeabb4a6a49fb9a0ab98a397b0a0a69ca9a2aa91a89b";
    private static final double EARTHS_DISTANCE = 0.5332283524866961d;
    private static final double EARTH_RESIZED_DISTANCE = 0.9105673748405818d;
    private static final double EARTH_CROPPED_DISTANCE = 0.7961021599566369d;
    private static final double EARTH_CAPTION_DISTANCE = 0.9999661575205901d;

    @Test
    public void testHashEarth1() throws IOException {
        Assert.assertEquals(EARTH1_HASH, getHashFor("earth1.jpg").toString());
    }

    @Test
    public void testHashEarth2() throws IOException {
        Assert.assertEquals(EARTH2_HASH, getHashFor("earth2.jpg").toString());
    }

    @Test
    public void testHashSerialization() throws IOException {
        RadialHash hash = getHashFor("earth1.jpg");
        String serialized = hash.toString();
        RadialHash hash2 = RadialHash.fromString(serialized);
        Assert.assertArrayEquals(hash.getCoefficients(), hash2.getCoefficients());
    }

    @Test
    public void testEarthDistance() throws IOException {
        Assert.assertEquals(EARTHS_DISTANCE,
                RadialHashAlgorithm.getSimilarity(
                        RadialHash.fromString(EARTH1_HASH),
                        RadialHash.fromString(EARTH2_HASH)
                ), 0.000001);
    }

    @Test
    public void testEarthResizedDistance() throws IOException {
        Assert.assertEquals(EARTH_RESIZED_DISTANCE,
                RadialHashAlgorithm.getSimilarity(
                        RadialHash.fromString(EARTH1_HASH),
                        getHashFor("earth1_resized.jpeg")
                ), 0.000001);
    }

    @Test
    public void testEarthCroppedDistance() throws IOException {
        Assert.assertEquals(EARTH_CROPPED_DISTANCE,
                RadialHashAlgorithm.getSimilarity(
                        RadialHash.fromString(EARTH1_HASH),
                        getHashFor("earth1_cropped.jpeg")
                ), 0.000001);
    }
    @Test
    public void testEarthCaptionDistance() throws IOException {
        Assert.assertEquals(EARTH_CAPTION_DISTANCE,
                RadialHashAlgorithm.getSimilarity(
                        RadialHash.fromString(EARTH1_HASH),
                        getHashFor("earth1_caption.jpeg")
                ), 0.000001);
    }

    private RadialHash getHashFor(String name) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        RadialHash hash = RadialHashAlgorithm.getHash(inputStream);
        inputStream.close();
        return hash;
    }
}
