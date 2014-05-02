package com.pragone.jphash.image;

import sun.misc.Unsafe;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

public class UnsafeSimpleGrayscaleImage {
    private static final int BYTE_SIZE = 8;
    private static final Unsafe unsafeInstance = getUnsafe();
    private long pointer;
    private int width;
    private int height;
    private int numPixels;

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public UnsafeSimpleGrayscaleImage(int width, int height) {
        this.width = width;
        this.height = height;
        this.numPixels = width*height;
        this.pointer = unsafeInstance.allocateMemory(width*height);
        unsafeInstance.setMemory(this.pointer, width * height, (byte) 0);
    }

    public UnsafeSimpleGrayscaleImage(BufferedImage image) {
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
        for (int y = 0; y < height; y++) {
            // Copy this horizontal line
            for (int i = 0; i < width; i++) {
                buffer[i] = getByte(width * y + i) & 0xFF;
                buffer_1[i] = (byte) (buffer[i] >> 1);  // buffer * 0.5
                buffer_2[i] = (byte) (buffer[i] >> 2);  // buffer * 0.25
            }
            // idx: 0
            long t = ((buffer[0] + buffer_1[1] + buffer_2[2])*585)>>10; // 1024/585 = 1.75042
            putByte(width * y, (byte) (t > 255 ? 255 : t));
            t = ((buffer_1[0] + buffer[1] + buffer_1[2] + buffer_2[3])*455)>>10;  // 1024/455 = 2.250549
            putByte(width*y+1,(byte) (t > 255 ? 255 : t));
            for (int x = 2; x < width-2; x++) {
                t = (int) (((long) (buffer_2[x-2] + buffer_1[x-1] + buffer[x] + buffer_1[x+1] + buffer_2[x+2]))*409)>>10; // 1024/409 = 2.503 ~ 1 + 2*0.5 + 2*0.25
                putByte(width*y+x, (byte) (t > 255 ? 255 : t));
            }
            int x = width-2;
            t = ((buffer_2[x-2] + buffer_1[x-1] + buffer[x] + buffer_1[x+1])*455)>>10; // 1024/455 = 2.250549
            putByte(width*y+x, (byte) (t > 255 ? 255 : t));
            x++;
            t = ((buffer_2[x-2] + buffer_1[x-1] + buffer[x])*585)>>10; // 1024/585 = 1.75042
            putByte(width*y+x, (byte) (t > 255 ? 255 : t));
        }

        // Now a vertical pass
        buffer = new int[height];
        buffer_1 = new int[height];
        buffer_2 = new int[height];
        for (int x = 0; x < width; x++) {
            // Copy this vertical line
            for (int i = 0; i < height; i++) {
                buffer[i] = getByte(width*i + x) & 0xFF;
                buffer_1[i] = (byte) (buffer[i] >> 1);  // buffer * 0.5
                buffer_2[i] = (byte) (buffer[i] >> 2);  // buffer * 0.25
            }
            // y = 0
            putByte(x,(byte) (((buffer[0] + buffer_1[1] + buffer_2[2])*585)>>10)); // 1024/585 = 1.75042
            // y = 1
            putByte(width+x,(byte) (((buffer_1[0] + buffer[1] + buffer_1[2] + buffer_2[3])*455)>>10)); // 1024/455 = 2.250549
            for (int y = 2; y < height-2; y++) {
                long t =  (((long) (buffer_2[y-2] + buffer_1[y-1] + buffer[y] + buffer_1[y+1] + buffer_2[y+2]))*409)>>10;
                putByte(width*y+x, (byte) (t > 255 ? 255 : t)); // 1024/409 = 2.503 ~ 1 + 2*0.5 + 2*0.25
            }
            int y = height-2;
            putByte(width*y+x, (byte) (((buffer_2[y-2] + buffer_1[y-1] + buffer[y] + buffer_1[y+1])*455)>>10)); // 1024/455 = 2.250549
            y++;
            putByte(width*y+x, (byte) (((buffer_2[y-2] + buffer_1[y-1] + buffer[y])*585)>>10)); // 1024/585 = 1.75042
        }
    }

    private byte getByte(long offset) {
        return unsafeInstance.getByte(this.pointer + offset);
    }
    
    private void putByte(long offset, byte value) {
        unsafeInstance.putByte(this.pointer + offset, value);
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
        long newData = unsafeInstance.allocateMemory(dest_width * dest_height);

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
                    unsafeInstance.putByte(newData + i * dest_width + j, (byte) (Cc & 0xFF));
                }
            }
        }
        long temp = this.pointer;
        this.pointer = newData;
        release(temp);
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
        return getByte(index) & 0xFF;
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
                putBytes(tempBuffer, 0, numPixels,0);
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
                    putByte(j++,(byte) y);
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
                long temp = (getByte(i) << 8) / maxPixel;
                putByte(i, (byte) (temp & 0xFF));
            }
            // V2: with int only math
//            for (int i =0; i < numPixels; i++) {
//                long temp = (getByte(i) * NORMALIZATION_APROX[this.maxPixel]) >> NORMALIZATION_DENOMINATOR_POWER;
//                putByte(i, (byte) (temp & 0xFF));
//            }
        }

    }

    private void putBytes(byte[] buffer, long bufferOffset, long numBytes, long toMemoryOffset) {
        for (long i = 0;  i < numBytes; i++) {
            putByte(toMemoryOffset + i, buffer[(int) (bufferOffset+i)]);
        }
    }

//    public void save(String path) {
//        BufferedImage temp = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);
//        byte[] buffer = new byte[width*height];
//        this.data.rewind();
//        getByte(buffer);
//        temp.getRaster().setDataElements(0,0,width,height,buffer);
//        try {
//            ImageIO.write(temp, "jpg", new File(path));
//        } catch (IOException e) {
//        }
//    }

    private static long allocate(long size) {
        long temp = unsafeInstance.allocateMemory(size);
        unsafeInstance.setMemory(temp, size, (byte) 0);
        return temp;
    }

    private static void release(long pointer) {
        unsafeInstance.freeMemory(pointer);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int get(int x, int y) {
        return getByte(width*y + x) & 0xFF;
    }

    public void dispose() {
        release(this.pointer);
    }

//        @Override
//        protected void finalize() throws Throwable {
//            Unsafe.getUnsafe().freeMemory(this.pointer);
//            super.finalize();
//        }
}