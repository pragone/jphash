package com.pragone.jphash.image;

import sun.misc.Cleaner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DisposingSimpleGrayscaleImage {
    private static final int BYTE_SIZE = 8;
    private static Map<Class<?>, Method> cleanerMethods = new HashMap<Class<?>, Method>();
    private int width;
    private int height;
    private ByteBuffer data;
    private int numPixels;

    static {
        ByteBuffer temp = ByteBuffer.allocateDirect(16);
        if (temp != null) {
            Method cleanerMethod = null;
            try {
                cleanerMethod = temp.getClass().getMethod("cleaner");
                cleanerMethod.setAccessible(true);
                cleanerMethods.put(temp.getClass(), cleanerMethod);

                try {
                    Cleaner cleaner = (Cleaner) cleanerMethod.invoke(temp);
                    cleaner.clean();
                } catch (Exception e) {
                    System.err.println("Couldnt clean up temporary Direct Byte Buffer. Possible memory leaks");
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    private static Method getCleanerMethod(Class<?> byteBufferClass) {
        Method temp = cleanerMethods.get(byteBufferClass);
        if (temp == null) {
            synchronized (DisposingSimpleGrayscaleImage.class) {
                if (!cleanerMethods.containsKey(byteBufferClass)) {
                    try {
                        Method cleanerMethod = temp.getClass().getMethod("cleaner");
                        cleanerMethod.setAccessible(true);
                        cleanerMethods.put(temp.getClass(), cleanerMethod);
                        return cleanerMethod;
                    } catch (NoSuchMethodException e) {
                        System.err.println("Couldnt clean up temporary Direct Byte Buffer. Possible memory leaks");
                    }
                }
            }
            temp = cleanerMethods.get(byteBufferClass);
        }
        return temp;
    }
//    private static final int[] NORMALIZATION_APROX;
//    private static final int NORMALIZATION_DENOMINATOR_POWER = 11; // 2048
//
//    static {
//        NORMALIZATION_APROX = new int[256];
//        double denominator = 1 << NORMALIZATION_DENOMINATOR_POWER;
//        for (int i = 1; i < 256; i++) {
//            double toAprox = ((double) 256)/i;
//            NORMALIZATION_APROX[i] = (int) Math.round(toAprox * denominator);
//        }
//    }

    public DisposingSimpleGrayscaleImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.numPixels = width*height;
        this.data = ByteBuffer.allocateDirect(width * height);

//            this.pointer = Unsafe.getUnsafe().allocateMemory(width*height);
//            Unsafe.getUnsafe().setMemory(this.pointer, width * height, (byte) 0);
    }

    public DisposingSimpleGrayscaleImage(BufferedImage image) {
        this(image.getWidth(), image.getHeight());
        loadImage(image);
        resizeToNextSize();
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
                buffer[i] = this.data.get(width*y + i) & 0xFF;
                buffer_1[i] = (byte) (buffer[i] >> 1);  // buffer * 0.5
                buffer_2[i] = (byte) (buffer[i] >> 2);  // buffer * 0.25
            }
            // idx: 0
            long t = ((buffer[0] + buffer_1[1] + buffer_2[2])*585)>>10; // 1024/585 = 1.75042
            this.data.put(width*y,(byte) (t > 255 ? 255 : t));
            t = ((buffer_1[0] + buffer[1] + buffer_1[2] + buffer_2[3])*455)>>10;  // 1024/455 = 2.250549
            this.data.put(width*y+1,(byte) (t > 255 ? 255 : t));
            for (int x = 2; x < width-2; x++) {
                t = (int) (((long) (buffer_2[x-2] + buffer_1[x-1] + buffer[x] + buffer_1[x+1] + buffer_2[x+2]))*409)>>10; // 1024/409 = 2.503 ~ 1 + 2*0.5 + 2*0.25
                this.data.put(width*y+x, (byte) (t > 255 ? 255 : t));
            }
            int x = width-2;
            t = ((buffer_2[x-2] + buffer_1[x-1] + buffer[x] + buffer_1[x+1])*455)>>10; // 1024/455 = 2.250549
            this.data.put(width*y+x, (byte) (t > 255 ? 255 : t));
            x++;
            t = ((buffer_2[x-2] + buffer_1[x-1] + buffer[x])*585)>>10; // 1024/585 = 1.75042
            this.data.put(width*y+x, (byte) (t > 255 ? 255 : t));
        }

        // Now a vertical pass
        buffer = new int[height];
        buffer_1 = new int[height];
        buffer_2 = new int[height];
        for (int x = 0; x < width; x++) {
            // Copy this vertical line
            for (int i = 0; i < height; i++) {
                buffer[i] = this.data.get(width*i + x) & 0xFF;
                buffer_1[i] = (byte) (buffer[i] >> 1);  // buffer * 0.5
                buffer_2[i] = (byte) (buffer[i] >> 2);  // buffer * 0.25
            }
            // y = 0
            this.data.put(x,(byte) (((buffer[0] + buffer_1[1] + buffer_2[2])*585)>>10)); // 1024/585 = 1.75042
            // y = 1
            this.data.put(width+x,(byte) (((buffer_1[0] + buffer[1] + buffer_1[2] + buffer_2[3])*455)>>10)); // 1024/455 = 2.250549
            for (int y = 2; y < height-2; y++) {
                long t =  (((long) (buffer_2[y-2] + buffer_1[y-1] + buffer[y] + buffer_1[y+1] + buffer_2[y+2]))*409)>>10;
                this.data.put(width*y+x, (byte) (t > 255 ? 255 : t)); // 1024/409 = 2.503 ~ 1 + 2*0.5 + 2*0.25
            }
            int y = height-2;
            this.data.put(width*y+x, (byte) (((buffer_2[y-2] + buffer_1[y-1] + buffer[y] + buffer_1[y+1])*455)>>10)); // 1024/455 = 2.250549
            y++;
            this.data.put(width*y+x, (byte) (((buffer_2[y-2] + buffer_1[y-1] + buffer[y])*585)>>10)); // 1024/585 = 1.75042
        }
    }

    private void resizeToNextSize() {
        int min = (width < height) ? width : height;
        min = getClosestSmallerPowerOf2(min);
        this.resize(min,min);
    }

    private int getClosestSmallerPowerOf2(int value) {
        int i = 0;
        int v = 1;
        while (v < value && i < 24) {
            i++;
            v=v<<1;
        }
        return v >> 1;
    }

    public void resize(int dest_width, int dest_height) {
        ByteBuffer newData = ByteBuffer.allocateDirect(dest_width * dest_height);

        double tx = ((double) width) / dest_width;
        double ty = ((double) height) / dest_height;

        int Cc;
        int C[] = new int[5];
        int d0, d2, d3, a0, a1, a2, a3;

        for (int i = 0; i < dest_height; ++i) {
            for (int j = 0; j < dest_width; ++j) {
                int x = (int) (tx * j);
                int y = (int) (ty * i);
                double dx = tx * j - x;
                double dy = ty * i - y;

                for (int jj = 0; jj <= 3; ++jj) {
                    d0 = safeGet((y - 1 + jj) * width + (x - 1)) -
                            safeGet((y - 1 + jj) * width + (x));
                    d2 = safeGet((y - 1 + jj) * width + (x + 1)) -
                            safeGet((y - 1 + jj) * width + (x));
                    d3 = safeGet((y - 1 + jj) * width + (x + 2)) -
                            safeGet((y - 1 + jj) * width + (x));
                    a0 = safeGet((y - 1 + jj) * width + (x));
                    a1 = (int) (-1.0 / 3 * d0 + d2 - 1.0 / 6 * d3);
                    a2 = (int) (1.0 / 2 * d0 + 1.0 / 2 * d2);
                    a3 = (int) (-1.0 / 6 * d0 - 1.0 / 2 * d2 + 1.0 / 6 * d3);
                    C[jj] = (int) (a0 + a1 * dx + a2 * dx * dx + a3 * dx * dx * dx);

                    d0 = C[0] - C[1];
                    d2 = C[2] - C[1];
                    d3 = C[3] - C[1];
                    a0 = C[1];
                    a1 = (int) (-1.0 / 3 * d0 + d2 -1.0 / 6 * d3);
                    a2 = (int) (1.0 / 2 * d0 + 1.0 / 2 * d2);
                    a3 = (int) (-1.0 / 6 * d0 - 1.0 / 2 * d2 + 1.0 / 6 * d3);
                    Cc = (int) (a0 + a1 * dy + a2 * dy * dy + a3* dy * dy * dy);
                    newData.put(i * dest_width + j, (byte) (Cc & 0xFF));
                }
            }
        }
        dispose();
        this.data = newData;
        this.width = dest_width;
        this.height = dest_height;
        this.numPixels = width*height;
    }


    private int safeGet(int index) {
        if (index < 0) {
            return 0;
        }
        if (index >= numPixels) {
            return 0;
        }
        return this.data.get(index) & 0xFF;
    }

    public void loadImage(BufferedImage image) {
        int numPixels = width*height;
        int numComponents = image.getColorModel().getNumComponents();
        int maxPixel = 0;
        if (image.getColorModel().getComponentSize(0) == BYTE_SIZE) {
            // Components are byte sized
            int bufferSize = numPixels*numComponents;
            byte[] tempBuffer ;
            try{
            tempBuffer = new byte[bufferSize];
            }catch (OutOfMemoryError e) {
                System.out.println("Died trying to allocate a buffer of size: " + bufferSize + "!!");
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
            image.getRaster().getDataElements(0,0,width,height,tempBuffer);

            if (numComponents == 1) {
                // Already byte gray
                this.data.put(tempBuffer, 0, numPixels);
            } else if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                int j = 0;
                for (int i = 0; i< bufferSize; i+= numComponents) {
                    // V1
//                        long yTemp = ((tempBuffer[i]& 0xFF)*BLUE_Y_COEFF +
//                                (tempBuffer[i+1]& 0xFF)*GREEN_Y_COEFF +
//                                (tempBuffer[i+2]& 0xFF)*RED_Y_COEFF);
//                        int y = (int) (((yTemp >> 10) & 0xFF) + ((yTemp >> 9) & 0x1));
//                    where:
//                    private static final long BLUE_Y_COEFF = (long) Math.floor(0.114d * 1024);
//                    private static final long GREEN_Y_COEFF = (long) Math.floor(0.587d * 1024);
//                    private static final long RED_Y_COEFF = (long) Math.floor(0.299d * 1024);

                    // V2: CImg version
                    int y = (((tempBuffer[i+2]& 0xFF) * 66 + (tempBuffer[i+1]& 0xFF) * 129 +
                            (tempBuffer[i]& 0xFF) *25) >> 8) + 16;

                    // if (y < 0) y = 0;
                    // else

                    if (y>255) y = 255;

                    if (y > maxPixel) {
                        maxPixel = y;
                    }
                    this.data.put(j++,(byte) y);
                }
            } else {
                throw new IllegalArgumentException("Can't work with this type of byte image: " + image.getType());
            }
        } else {
            throw new IllegalArgumentException("Can't work with non-byte image buffers");
        }
        if (maxPixel > 0) {
            // Let's normalize amount of light
            // V1: with double math
            for (int i =0; i < numPixels; i++) {
                long temp = (this.data.get(i) << 8) / maxPixel;
                this.data.put(i, (byte) (temp & 0xFF));
            }
            // V2: with int only math
//            for (int i =0; i < numPixels; i++) {
//                long temp = (this.data.get(i) * NORMALIZATION_APROX[this.maxPixel]) >> NORMALIZATION_DENOMINATOR_POWER;
//                this.data.put(i, (byte) (temp & 0xFF));
//            }
        }

    }

    public void save(String path) {
        BufferedImage temp = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
        byte[] buffer = new byte[width*height];
        this.data.rewind();
        this.data.get(buffer);
        temp.getRaster().setDataElements(0,0,width,height,buffer);
        try {
            ImageIO.write(temp, "jpg", new File(path));
        } catch (IOException e) {
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int get(int x, int y) {
        return this.data.get(width*y + x) & 0xFF;
    }

    public void dispose() {
        if (this.data != null && this.data.isDirect()) {

            Method cleanerMethod = getCleanerMethod(this.data.getClass());
            try {
                Cleaner cleaner = (Cleaner) cleanerMethod.invoke(this.data);
                cleaner.clean();
            } catch (InvocationTargetException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
    }

//        @Override
//        protected void finalize() throws Throwable {
//            Unsafe.getUnsafe().freeMemory(this.pointer);
//            super.finalize();
//        }
}