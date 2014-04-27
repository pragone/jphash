package com.pragone.jphash.image.radial;

import com.pragone.jphash.image.SimpleGrayscaleImage;

import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 25/04/2014
 * Time: 5:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class RadialHashAlgorithm {
    private static final float[] BLUR_MATRIX = {
            0.111f, 0.111f, 0.111f,
            0.111f, 0.111f, 0.111f,
            0.111f, 0.111f, 0.111f,
    };
    private static final int DEFAULT_NUM_PROJECTIONS = 180;
    private static final double SQRT_TWO = Math.sqrt(2);
    private static final int UCHAR_MAX = 255;
    private static final double[] THETA_180;
    private static final double[] TAN_THETA_180;

    static {
        THETA_180 = new double[180];
        TAN_THETA_180 = new double[180];
        for (int i = 0; i < 180; i++) {
            THETA_180[i] = i* Math.PI/180;
            TAN_THETA_180[i] = Math.tan(THETA_180[i]);
        }
    }

    public static RadialHash getHash(File file) throws IOException {
        return getHash(new FileInputStream(file));
    }

    public static RadialHash getHash(InputStream inputStream) throws IOException {
        BufferedImage img = ImageIO.read(inputStream);
        return getHash(img);
    }

    public static RadialHash getHash2(InputStream inputStream) throws IOException {
        BufferedImage img = ImageIO.read(inputStream);
        return getHash2(img);
    }


    public static RadialHash getHash3(String file) throws IOException {
        return getHash(new File(file));
    }

    public static RadialHash getHash3(File file) throws IOException {
        return getHash(new FileInputStream(file));
    }

    public static RadialHash getHash3(InputStream inputStream) throws IOException {
        BufferedImage img = ImageIO.read(inputStream);
        return getHash3(img);
    }

    private static BufferedImage preprocessImage(BufferedImage image) {
        // V3



        // V2
        BufferedImage grayscale = new BufferedImage(image.getWidth(), image
                .getHeight(), BufferedImage.TYPE_INT_RGB);
        ColorConvertOp grayscaleOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        grayscaleOp.filter(image, grayscale);

//        System.out.println("Grayscale");
//        printMiddleLine(grayscale);
//        saveImage(grayscale);

        BufferedImage blurred = new BufferedImage(image.getWidth(), image
                .getHeight(), BufferedImage.TYPE_INT_RGB);

        // Blur the image
        ConvolveOp op = new ConvolveOp( new Kernel(3, 3, BLUR_MATRIX) );
        op.filter(grayscale, blurred);
//        System.out.println("Blurred");
//        printMiddleLine(blurred);

        BufferedImage result = new BufferedImage(image.getWidth(), image
                .getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        new ColorConvertOp(null).filter(blurred, result);
        return result;


        // V1
//
//        BufferedImage intImg = new BufferedImage(image.getWidth(), image
//                .getHeight(), BufferedImage.TYPE_INT_RGB);
//        new ColorConvertOp(null).filter(image,intImg);
//
//        BufferedImage blurred = new BufferedImage(image.getWidth(), image
//                .getHeight(), BufferedImage.TYPE_INT_RGB);
//
//        // Blur the image
//        ConvolveOp op = new ConvolveOp( new Kernel(3, 3, BLUR_MATRIX) );
//        op.filter(intImg, blurred);
//
//        //ImageIO.write(blurred, "jpg", new File("/tmp/blurred.jpg"));
//        BufferedImage result = new BufferedImage(image.getWidth(), image
//                .getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//
//        //Convert the image to grayscale
//        ColorConvertOp grayscaleOp = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
//        grayscaleOp.filter(blurred, blurred);
//
//        new ColorConvertOp(null).filter(blurred, result);
//
////        try {
////            ImageIO.write(result, "jpg", new File("/tmp/grayscaled.jpg"));
////        } catch (IOException e) {
////        }
//        return blurred;
    }

    private static void saveImage(BufferedImage image) {
//        if (image.getType() != BufferedImage.TYPE_BYTE_GRAY) {
//            BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
//            new ColorConvertOp(null).filter(image, temp);
//            image = temp;
//        }
        try {
            ImageIO.write(image, "jpg", new File("/tmp/grayscaled.jpg"));
        } catch (IOException e) {
        }

    }

    private static RadialHash getHash(BufferedImage img) throws IOException {
        long start = System.currentTimeMillis();
        BufferedImage blurredGrayImg = preprocessImage(img);
        long endPreprocess = System.currentTimeMillis();
        Projections projections = calculateProjections(blurredGrayImg);
        long endProjections = System.currentTimeMillis();
        Features features = calculateFeatures(projections);
        long endFeatures = System.currentTimeMillis();
        RadialHash temp = calculateHash(features);
        long endHash = System.currentTimeMillis();
        System.out.println("getHash times: preprocessing: " + (endPreprocess - start) +
                ", projections: " + (endProjections-endPreprocess) +
                ", features: " + (endFeatures-endProjections) +
                ", hash: " + (endHash-endFeatures) +
                ", TOTAL: " + (endHash-start));
        return temp;
    }

    private static RadialHash getHash2(BufferedImage img) throws IOException {
        long start = System.currentTimeMillis();
        SimpleGrayscaleImage grayscaleImage = new SimpleGrayscaleImage(img);
        long endPreprocess = System.currentTimeMillis();
        Projections projections = calculateProjections(grayscaleImage);
        long endProjections = System.currentTimeMillis();
        Features features = calculateFeatures(projections);
        long endFeatures = System.currentTimeMillis();
        RadialHash temp = calculateHash(features);
        long endHash = System.currentTimeMillis();
        System.out.println("getHash2 times: preprocessing: " + (endPreprocess - start) +
                ", projections: " + (endProjections-endPreprocess) +
                ", features: " + (endFeatures-endProjections) +
                ", hash: " + (endHash-endFeatures) +
                ", TOTAL: " + (endHash-start));
        return temp;
    }

    private static RadialHash getHash3(BufferedImage img) throws IOException {
        long start = System.currentTimeMillis();
        SimpleGrayscaleImage grayscaleImage = new SimpleGrayscaleImage(img);
        long endPreprocess = System.currentTimeMillis();
        Projections projections = calculate180Projections(grayscaleImage);
        long endProjections = System.currentTimeMillis();
        Features features = calculateFeatures(projections);
        long endFeatures = System.currentTimeMillis();
        RadialHash temp = calculateHash(features);
        long endHash = System.currentTimeMillis();
        System.out.println("getHash2 times: preprocessing: " + (endPreprocess - start) +
                ", projections: " + (endProjections-endPreprocess) +
                ", features: " + (endFeatures-endProjections) +
                ", hash: " + (endHash-endFeatures) +
                ", TOTAL: " + (endHash-start));
        return temp;
    }

    private static RadialHash calculateHash(Features features) {
        int N = features.getNumberOfProjections();
        int nb_coeffs = 40;

        RadialHash digest = new RadialHash(nb_coeffs);

        double[] R = features.features;

        byte[] D = digest.getCoefficients();

        double D_temp[] = new double[nb_coeffs];
        double max = 0.0;
        double min = 0.0;
        for (int k = 0;k<nb_coeffs;k++){
            double sum = 0.0;
            for (int n=0;n<N;n++){
                double temp = R[n]*Math.cos((Math.PI * (2 * n + 1) * k) / (2 * N));
                sum += temp;
            }
            if (k == 0)
                D_temp[k] = sum/Math.sqrt((double) N);
            else
                D_temp[k] = sum*SQRT_TWO/Math.sqrt((double) N);
            if (D_temp[k] > max)
                max = D_temp[k];
            if (D_temp[k] < min)
                min = D_temp[k];
        }

        for (int i=0;i<nb_coeffs;i++){

            D[i] = (byte)(UCHAR_MAX*(D_temp[i] - min)/(max - min));

        }
        return digest;
    }

    private static Features calculateFeatures(Projections projections) {

        int[][] projection_map = projections.projections;
        int[] nb_perline = projections.nb_pix_perline;
        int N = projections.getNumberOfProjections();
        int D = projections.getMaxDimension();

        Features features = new Features(N);

        double[] feat_v = features.features;
        double sum = 0.0;
        double sum_sqd = 0.0;
        for (int k = 0; k < N; k++) {
            double line_sum = 0.0;
            double line_sum_sqd = 0.0;
            int nb_pixels = nb_perline[k];
            for (int i = 0; i < D; i++) {
                line_sum += projection_map[k][i];
                line_sum_sqd += projection_map[k][i] * projection_map[k][i];
            }
            feat_v[k] = (line_sum_sqd / nb_pixels) - (line_sum * line_sum) / (nb_pixels * nb_pixels);
            sum += feat_v[k];
            sum_sqd += feat_v[k] * feat_v[k];
        }
        double mean = sum / N;
        double var = Math.sqrt((sum_sqd / N) - (sum * sum) / (N * N));

        for (int i = 0; i < N; i++) {
            feat_v[i] = (feat_v[i] - mean) / var;
        }

        return features;
    }

    private static Projections calculateProjections(BufferedImage img) {
        return calculateProjections(img, DEFAULT_NUM_PROJECTIONS);
    }

    private static Projections calculateProjections(SimpleGrayscaleImage image) {
        return calculateProjections(image, DEFAULT_NUM_PROJECTIONS);
    }

    private static Projections calculateProjections(SimpleGrayscaleImage img, int N) {
        int width = img.getWidth();
        int height = img.getHeight();
        int D = (width > height)?width:height;
        float x_center = (float)width/2;
        float y_center = (float)height/2;
        int x_off = (int)Math.floor(x_center + (x_center >= 0 ? 0.5 : -0.5));
        int y_off = (int)Math.floor(y_center + (y_center >= 0 ? 0.5 : -0.5));

        Projections projections = new Projections(N,D);

        int[][] ptr_radon_map = projections.projections;
        int[] nb_per_line = projections.nb_pix_perline;

        for (int k=0;k<N/4+1;k++) {
            double theta = k* Math.PI/N;
            double alpha = Math.tan(theta);
            for (int x=0;x < D;x++) {
                double y = alpha*(x-x_off);
                int yd = (int)Math.floor(y + (y >= 0 ? 0.5 : -0.5));
                if ((yd + y_off >= 0)&&(yd + y_off < height) && (x < width)) {
                    ptr_radon_map[k][x] = img.get(x, yd + y_off);
                    nb_per_line[k] += 1;
                }
                if ((yd + x_off >= 0) && (yd + x_off < width) && (k != N/4) && (x < height)) {
                    ptr_radon_map[N/2-k][x] = img.get(yd + x_off, x);
                    nb_per_line[N/2-k] += 1;
                }
            }
        }
        int j= 0;
        for (int k=3*N/4;k<N;k++){
            double theta = k*Math.PI/N;
            double alpha = Math.tan(theta);
            for (int x=0;x < D;x++){
                double y = alpha*(x-x_off);
                int yd = (int)Math.floor(y + (y >= 0 ? 0.5 : -0.5));
                if ((yd + y_off >= 0)&&(yd + y_off < height) && (x < width)){
                    ptr_radon_map[k][x] = img.get(x, yd + y_off);
                    nb_per_line[k] += 1;
                }
                if ((y_off - yd >= 0)&&(y_off - yd<width)&&(2*y_off-x>=0)&&(2*y_off-x<height)&&(k!=3*N/4)){
                    ptr_radon_map[k-j][x] = img.get(-yd + y_off, -(x - y_off) + y_off);
                    nb_per_line[k-j] += 1;
                }

            }
            j += 2;
        }
        return projections;
    }

    private static Projections calculate180Projections(SimpleGrayscaleImage img) {
        int width = img.getWidth();
        int N = 180;
        int height = img.getHeight();
        int D = (width > height)?width:height;
        int x_off = (width >> 1) + (width & 0x1); // round(width/2) but only with integer operations
        int y_off = (height >> 1) + (height & 0x1); // round(height/2) but only with integer operations

        Projections projections = new Projections(N,D);

        int[][] ptr_radon_map = projections.projections;
        int[] nb_per_line = projections.nb_pix_perline;

        for (int k=0;k<N/4+1;k++) {
            double alpha = TAN_THETA_180[k];
            for (int x=0;x < D;x++) {
                double y = alpha*(x-x_off);
                int yd = (int)Math.floor(y + (y >= 0 ? 0.5 : -0.5));
                if ((yd + y_off >= 0)&&(yd + y_off < height) && (x < width)) {
                    ptr_radon_map[k][x] = img.get(x, yd + y_off);
                    nb_per_line[k] += 1;
                }
                if ((yd + x_off >= 0) && (yd + x_off < width) && (k != N/4) && (x < height)) {
                    ptr_radon_map[N/2-k][x] = img.get(yd + x_off, x);
                    nb_per_line[N/2-k] += 1;
                }
            }
        }
        int j= 0;
        for (int k=3*N/4;k<N;k++){
            double alpha = TAN_THETA_180[k];
            for (int x=0;x < D;x++){
                double y = alpha*(x-x_off);
                int yd = (int)Math.floor(y + (y >= 0 ? 0.5 : -0.5));
                if ((yd + y_off >= 0)&&(yd + y_off < height) && (x < width)){
                    ptr_radon_map[k][x] = img.get(x, yd + y_off);
                    nb_per_line[k] += 1;
                }
                if ((y_off - yd >= 0)&&(y_off - yd<width)&&(2*y_off-x>=0)&&(2*y_off-x<height)&&(k!=3*N/4)){
                    ptr_radon_map[k-j][x] = img.get(-yd + y_off, -(x - y_off) + y_off);
                    nb_per_line[k-j] += 1;
                }

            }
            j += 2;
        }
        return projections;
    }


    /**
         *
         * @param img
         * @param N number of projections
         * @return
         */
    private static Projections calculateProjections(BufferedImage img, int N) {
        int width = img.getWidth();
        int height = img.getHeight();
        int D = (width > height)?width:height;
        float x_center = (float)width/2;
        float y_center = (float)height/2;
        int x_off = (int)Math.floor(x_center + (x_center >= 0 ? 0.5 : -0.5));
        int y_off = (int)Math.floor(y_center + (y_center >= 0 ? 0.5 : -0.5));

        Projections projections = new Projections(N,D);

        int[][] ptr_radon_map = projections.projections;
        int[] nb_per_line = projections.nb_pix_perline;

        for (int k=0;k<N/4+1;k++) {
            double theta = k* Math.PI/N;
            double alpha = Math.tan(theta);
            for (int x=0;x < D;x++) {
                double y = alpha*(x-x_off);
                int yd = (int)Math.floor(y + (y >= 0 ? 0.5 : -0.5));
                if ((yd + y_off >= 0)&&(yd + y_off < height) && (x < width)) {
                    ptr_radon_map[k][x] = img.getRGB(x, yd + y_off);
                    nb_per_line[k] += 1;
                }
                if ((yd + x_off >= 0) && (yd + x_off < width) && (k != N/4) && (x < height)) {
                    ptr_radon_map[N/2-k][x] = img.getRGB(yd + x_off, x);
                    nb_per_line[N/2-k] += 1;
                }
            }
        }
        int j= 0;
        for (int k=3*N/4;k<N;k++){
            double theta = k*Math.PI/N;
            double alpha = Math.tan(theta);
            for (int x=0;x < D;x++){
                double y = alpha*(x-x_off);
                int yd = (int)Math.floor(y + (y >= 0 ? 0.5 : -0.5));
                if ((yd + y_off >= 0)&&(yd + y_off < height) && (x < width)){
                    ptr_radon_map[k][x] = img.getRGB(x, yd + y_off);
                    nb_per_line[k] += 1;
                }
                if ((y_off - yd >= 0)&&(y_off - yd<width)&&(2*y_off-x>=0)&&(2*y_off-x<height)&&(k!=3*N/4)){
                    ptr_radon_map[k-j][x] = img.getRGB(-yd + y_off, -(x - y_off) + y_off);
                    nb_per_line[k-j] += 1;
                }

            }
            j += 2;
        }
        return projections;
    }

//    public static void main(String[] args) throws IOException {
//        String[] imgs = new String[] {"test1.jpg", "test2.jpg", "test3.jpg", "test4.jpg"};
//        RadialHash[] hashv1 = new RadialHash[imgs.length];
//        for (int i = 0; i < imgs.length; i++) {
//            InputStream is = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream(imgs[i]);
//            hashv1[i] = getHash(is);
//            System.out.println("Hashv1 for " + imgs[i] + ": " + Base64.encodeBase64String(hashv1[i].getCoefficients()));
//            is.close();
//        }
//
//        RadialHash[] hashv2 = new RadialHash[imgs.length];
//        for (int i = 0; i < imgs.length; i++) {
//            InputStream is = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream(imgs[i]);
//            hashv2[i] = getHash2(is);
//            System.out.println("Hashv2 for " + imgs[i] + ": " + Base64.encodeBase64String(hashv2[i].getCoefficients()));
//            is.close();
//        }
//
//        RadialHash[] hashv3 = new RadialHash[imgs.length];
//        for (int i = 0; i < imgs.length; i++) {
//            InputStream is = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream(imgs[i]);
//            hashv3[i] = getHash3(is);
//            System.out.println("Hashv3 for " + imgs[i] + ": " + Base64.encodeBase64String(hashv3[i].getCoefficients()));
//            is.close();
//        }
////
////        InputStream is = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream("test1.jpg");
////        RadialHash hash = getHash(is);
////
////        InputStream is2 = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream("test1.jpg");
////        RadialHash hash2 = getHash2(is2);
////
////        is = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream("test1.jpg");
////        BufferedImage bi = ImageIO.read(is);
////        long start = System.currentTimeMillis();
////        SimpleGrayscaleImage temp = new SimpleGrayscaleImage(bi);
////        System.out.println("Grayscaled in " + (System.currentTimeMillis() - start));
////        temp.save("/tmp/simplegray.jpg");
//
////        System.out.println("Hash: " + Base64.encodeBase64String(hash.getCoefficients()));
////        System.out.println("Hash2: " + Base64.encodeBase64String(hash2.getCoefficients()));
////
////        InputStream is2 = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream("test2.jpg");
////        RadialHash hash2 = getHash(is2);
////        System.out.println("Hash: " + Base64.encodeBase64String(hash2.getCoefficients()));
////
////        InputStream is3 = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream("test3.jpg");
////        RadialHash hash3 = getHash(is3);
////        System.out.println("Hash: " + Base64.encodeBase64String(hash3.getCoefficients()));
////
////        InputStream is4 = RadialHashAlgorithm.class.getClassLoader().getResourceAsStream("test4.jpg");
////        RadialHash hash4 = getHash(is4);
////        System.out.println("Hash: " + Base64.encodeBase64String(hash4.getCoefficients()));
////
//
//        for (int i = 0; i < (hashv1.length-1); i++) {
//            for (int j= i ; j < hashv1.length; j++) {
//                System.out.println("v1 (" + (i+1) + "," + (j+1) + "): " + calculateSimilarity(hashv1[i], hashv1[j]));
//            }
//        }
//        for (int i = 0; i < (hashv1.length-1); i++) {
//            for (int j= i ; j < hashv1.length; j++) {
//                System.out.println("v2 (" + (i+1) + "," + (j+1) + "): " + calculateSimilarity(hashv2[i], hashv2[j]));
//            }
//        }
//        for (int i = 0; i < (hashv1.length-1); i++) {
//            for (int j= i ; j < hashv1.length; j++) {
//                System.out.println("v3 (" + (i+1) + "," + (j+1) + "): " + calculateSimilarity(hashv3[i], hashv3[j]));
//            }
//        }
//    }

    private static double calculateSimilarity(RadialHash hash1, RadialHash hash2) {

        int N = hash1.getCoefficients().length;

        byte[] x_coeffs = hash1.getCoefficients();
        byte[] y_coeffs = hash2.getCoefficients();

        double r[] = new double[N];
        double sumx = 0.0;
        double sumy = 0.0;
        for (int i=0;i < N;i++){
            sumx += x_coeffs[i] & 0xFF;
            sumy += y_coeffs[i] & 0xFF;
        }
        double meanx = sumx/N;
        double meany = sumy/N;
        double max = 0;
        for (int d=0;d<N;d++){
            double num = 0.0;
            double denx = 0.0;
            double deny = 0.0;
            for (int i=0;i<N;i++){
                num  += (x_coeffs[i]-meanx)*(y_coeffs[(N+i-d)%N]-meany);
                denx += Math.pow((x_coeffs[i] - meanx), 2);
                deny += Math.pow((y_coeffs[(N + i - d) % N] - meany), 2);
            }
            r[d] = num/Math.sqrt(denx * deny);
            if (r[d] > max)
                max = r[d];
        }
        return max;  //To change body of created methods use File | Settings | File Templates.
    }

    private static class Projections {

        public final int[] nb_pix_perline;
        private final int[][] projections;

        public Projections(int numberOfProjections, int maxDimension) {
            this.nb_pix_perline = new int[numberOfProjections];
            this.projections = new int[numberOfProjections][maxDimension];
        }

        public int getNumberOfProjections() {
            return this.nb_pix_perline.length;
        }

        public int getMaxDimension() {
            return projections[0].length;
        }
    }

    private static class Features {
        public final double[] features;

        public Features(int numberOfProjections) {
            this.features = new double[numberOfProjections];
        }

        public int getNumberOfProjections() {
            return this.features.length;
        }
    }
}
