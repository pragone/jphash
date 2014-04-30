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

    private static final String EARTH1_HASH = "7cff4651003bc4a2b37a547a978b7b6e7a687c808d7c8688948983767f808c7d79727a7376757473";
    private static final String EARTH2_HASH = "a3ff195b0048ac9be693c3a394a1bcb7ab9bb5b4baababa8c0a4b19daa9eb2a7aba2aea7af97ada3";
    private static final double EARTHS_DISTANCE = 0.7189180787765737d;
    private static final double EARTH_RESIZED_DISTANCE = 0.9999806438624114d;
    private static final double EARTH_CROPPED_DISTANCE = 0.8785204004702291d;
    private static final double EARTH_CAPTION_DISTANCE = 0.9590921706328405d;

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
                        getHashFor("earth1.jpg"),
                        getHashFor("earth1_resized.jpeg")
                ), 0.000001);
    }

    @Test
    public void testEarthCroppedDistance() throws IOException {
        Assert.assertEquals(EARTH_CROPPED_DISTANCE,
                RadialHashAlgorithm.getSimilarity(
                        getHashFor("earth1.jpg"),
                        getHashFor("earth1_cropped.jpeg")
                ), 0.000001);
    }
    @Test
    public void testEarthCaptionDistance() throws IOException {
        Assert.assertEquals(EARTH_CAPTION_DISTANCE,
                RadialHashAlgorithm.getSimilarity(
                        getHashFor("earth1.jpg"),
                        getHashFor("earth1_caption.jpeg")
                ), 0.000001);
    }
//
//    @Test
//    public void showResizeResult_earth1() throws IOException {
//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("earth1.jpg");
//        BufferedImage img = ImageIO.read(inputStream);
//        new SimpleGrayscaleImage(img).save("/tmp/earth1_resized.jpg");
//        inputStream.close();
//    }
//
//    @Test
//    public void showResizeResult_earth1_caption() throws IOException {
//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("earth1_caption.jpeg");
//        BufferedImage img = ImageIO.read(inputStream);
//        new SimpleGrayscaleImage(img).save("/tmp/earth1_caption_resized.jpg");
//        inputStream.close();
//    }
//    @Test
//    public void showResizeResult_earth1_resized() throws IOException {
//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("earth1_resized.jpeg");
//        BufferedImage img = ImageIO.read(inputStream);
//        new SimpleGrayscaleImage(img).save("/tmp/earth1_resized_resized.jpg");
//        inputStream.close();
//    }

    private static double norm2(byte[] q) {
        if (q == null) {
            throw new IllegalArgumentException("Norm can only be calculated on a non-null vector");
        }
        double resp = 0;
        for (int i = 0; i < q.length; i++) {
            resp += q[i]*q[i];
        }
        return Math.sqrt(resp);
    }

//    @Test
//    public void checkNorms() throws IOException {
//        System.out.println(norm2(getHashFor("earth1.jpg").getCoefficients()));
//        System.out.println(norm2(getHashFor("earth1_caption.jpeg").getCoefficients()));
//        System.out.println(norm2(getHashFor("earth1_cropped.jpeg").getCoefficients()));
//    }

    private RadialHash getHashFor(String name) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(name);
        RadialHash hash = RadialHashAlgorithm.getHash(inputStream);
        inputStream.close();
        return hash;
    }
}
