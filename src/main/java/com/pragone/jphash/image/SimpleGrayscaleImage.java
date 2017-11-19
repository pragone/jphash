package com.pragone.jphash.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class SimpleGrayscaleImage {
    private int width;
    private int height;
    private ByteBuffer data;
    private int numPixels;

    public SimpleGrayscaleImage(BufferedImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.numPixels = width * height;

        loadImage(image);
        blur();
    }

    public void blur() {
        if (width <= 6 || height <= 6) {
            return;
        }
        // First a horizontal pass
        int[] buffer = new int[width];
        int[] buffer_1 = new int[width];
        int[] buffer_2 = new int[width];
        this.data.rewind();
        for (int y = 0; y < height; y++) {
            // Copy this horizontal line
            for (int i = 0; i < width; i++) {
                buffer[i] = this.data.get(width * y + i) & 0xFF;
                buffer_1[i] = (byte) (buffer[i] >> 1);  // buffer * 0.5
                buffer_2[i] = (byte) (buffer[i] >> 2);  // buffer * 0.25
            }
            // idx: 0
            long t = ((buffer[0] + buffer_1[1] + buffer_2[2]) * 585) >> 10; // 1024/585 = 1.75042
            this.data.put(width * y, (byte) (t > 255 ? 255 : t));
            t = ((buffer_1[0] + buffer[1] + buffer_1[2] + buffer_2[3]) * 455) >> 10;  // 1024/455 = 2.250549
            this.data.put(width * y + 1, (byte) (t > 255 ? 255 : t));
            for (int x = 2; x < width - 2; x++) {
                t = (int) (((long) (buffer_2[x - 2] + buffer_1[x - 1] + buffer[x] + buffer_1[x + 1] + buffer_2[x + 2])) * 409) >> 10; // 1024/409 = 2.503 ~ 1 + 2*0.5 + 2*0.25
                this.data.put(width * y + x, (byte) (t > 255 ? 255 : t));
            }
            int x = width - 2;
            t = ((buffer_2[x - 2] + buffer_1[x - 1] + buffer[x] + buffer_1[x + 1]) * 455) >> 10; // 1024/455 = 2.250549
            this.data.put(width * y + x, (byte) (t > 255 ? 255 : t));
            x++;
            t = ((buffer_2[x - 2] + buffer_1[x - 1] + buffer[x]) * 585) >> 10; // 1024/585 = 1.75042
            this.data.put(width * y + x, (byte) (t > 255 ? 255 : t));
        }

        // Now a vertical pass
        buffer = new int[height];
        buffer_1 = new int[height];
        buffer_2 = new int[height];
        for (int x = 0; x < width; x++) {
            // Copy this vertical line
            for (int i = 0; i < height; i++) {
                buffer[i] = this.data.get(width * i + x) & 0xFF;
                buffer_1[i] = (byte) (buffer[i] >> 1);  // buffer * 0.5
                buffer_2[i] = (byte) (buffer[i] >> 2);  // buffer * 0.25
            }
            // y = 0
            this.data.put(x, (byte) (((buffer[0] + buffer_1[1] + buffer_2[2]) * 585) >> 10)); // 1024/585 = 1.75042
            // y = 1
            this.data.put(width + x, (byte) (((buffer_1[0] + buffer[1] + buffer_1[2] + buffer_2[3]) * 455) >> 10)); // 1024/455 = 2.250549
            for (int y = 2; y < height - 2; y++) {
                long t = (((long) (buffer_2[y - 2] + buffer_1[y - 1] + buffer[y] + buffer_1[y + 1] + buffer_2[y + 2])) * 409) >> 10;
                this.data.put(width * y + x, (byte) (t > 255 ? 255 : t)); // 1024/409 = 2.503 ~ 1 + 2*0.5 + 2*0.25
            }
            int y = height - 2;
            this.data.put(width * y + x, (byte) (((buffer_2[y - 2] + buffer_1[y - 1] + buffer[y] + buffer_1[y + 1]) * 455) >> 10)); // 1024/455 = 2.250549
            y++;
            this.data.put(width * y + x, (byte) (((buffer_2[y - 2] + buffer_1[y - 1] + buffer[y]) * 585) >> 10)); // 1024/585 = 1.75042
        }
    }

    private static final int RESIZE_WIDTH = 1024;
    private static final int RESIZE_HEIGHT = 1024;

    public void loadImage(BufferedImage image) {
        try {
            BufferedImage resizedImage = new BufferedImage(RESIZE_WIDTH, RESIZE_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(image, 0, 0, RESIZE_WIDTH, RESIZE_HEIGHT, null);
            g.dispose();

            numPixels = (RESIZE_WIDTH * RESIZE_HEIGHT);
            width = RESIZE_WIDTH;
            height = RESIZE_HEIGHT;

            int totalPixels = numPixels * resizedImage.getColorModel().getNumComponents();

            byte[] tempBuffer = new byte[totalPixels];
            resizedImage.getRaster().getDataElements(0, 0, RESIZE_WIDTH, RESIZE_HEIGHT, tempBuffer);

            data = ByteBuffer.wrap(tempBuffer);
        } catch (OutOfMemoryError e) {
            System.err.println("Died trying to allocate a buffer for image. Please increase heap size!!");
            throw e;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int get(int x, int y) {
        return this.data.get(width * y + x) & 0xFF;
    }

}