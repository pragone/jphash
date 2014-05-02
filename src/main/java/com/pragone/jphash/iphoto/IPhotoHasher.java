package com.pragone.jphash.iphoto;

import com.pragone.jphash.image.radial.RadialHash;
import com.pragone.jphash.image.radial.RadialHashAlgorithm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 1/05/2014
 * Time: 11:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class IPhotoHasher {
    private static int numHashes = 0;
    private static File resp;
    private static BufferedWriter bw;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Specify dir to look for images and file to save results to");
            return;
        }
        File f = new File(args[0]);
        resp = new File(args[1]);
        bw = new BufferedWriter(new FileWriter(resp));
        walkDir(f);
        bw.close();
    }

    private static void walkDir(File dir) {
        if (!dir.isDirectory()) {
            return;
        }
        for (File f : dir.listFiles()) {
            if (f.isDirectory() && !f.equals(dir)) {
                walkDir(f);
            } else {
                int pos = f.getName().lastIndexOf('.');
                if (pos > 0) {
                    String ext = f.getName().substring(pos+1);
                    if (ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg")) {
                        process(f);
                    }
                }
            }
        }
    }

    private static void process(File file) {
        try {
            RadialHash hash = RadialHashAlgorithm.getHash(file);
            bw.write(hash + "\t" + file.getAbsolutePath() + "\n");
            System.out.println(file.getAbsolutePath() + "\t" + hash);
            numHashes++;
            if ((numHashes % 100) == 0) {
                bw.flush();
                bw.close();
                bw = new BufferedWriter(new FileWriter(resp, true));
            }
        } catch (IOException e) {
            System.out.println(file.getAbsolutePath() + "\tERROR");
        }
    }
}
