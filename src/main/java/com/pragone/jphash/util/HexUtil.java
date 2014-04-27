package com.pragone.jphash.util;

/**
 * Created with IntelliJ IDEA.
 * User: pragone
 * Date: 27/04/2014
 * Time: 6:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class HexUtil {

    public static String byteArrayToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toHexString((b & 0xF0)>>4));
            sb.append(Integer.toHexString(b&0xF));
        }
        return sb.toString();
    }

    public static byte[] stringToByteArray(String string, byte[] destination) {
        if (destination == null) {
            destination = new byte[string.length()/2];
        }
        int j = 0;
        for (int i = 0; i < string.length(); i+=2) {
            destination[j++] = Integer.valueOf(string.substring(i,i+2), 16).byteValue();
        }
        return destination;
    }
}
