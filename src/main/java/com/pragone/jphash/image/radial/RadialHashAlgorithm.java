package com.pragone.jphash.image.radial;

import com.pragone.jphash.image.SimpleGrayscaleImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: pragone
 * Date: 25/04/2014
 * Time: 5:21 PM
 */
public class RadialHashAlgorithm {
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

    public static RadialHash getHash(String file) throws IOException {
        return getHash(new File(file));
    }

    public static RadialHash getHash(File file) throws IOException {
        FileInputStream is = new FileInputStream(file);
        try {
            return getHash(is);
        } finally {
            is.close();;
        }
    }

    public static RadialHash getHash(InputStream inputStream) throws IOException {
        BufferedImage img = ImageIO.read(inputStream);
        return getHash(img);
    }

    public static RadialHash getHash(BufferedImage img) throws IOException {
        SimpleGrayscaleImage grayscaleImage = new SimpleGrayscaleImage(img);
        Projections projections = calculate180Projections(grayscaleImage);
//        grayscaleImage.dispose();
        Features features = calculateFeatures(projections);
        RadialHash temp = calculateHash(features);
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

    public static double getSimilarity(RadialHash hash1, RadialHash hash2) {

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
