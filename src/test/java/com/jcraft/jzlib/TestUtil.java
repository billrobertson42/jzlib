package com.jcraft.jzlib;

import java.util.Random;

public class TestUtil {

    public static byte[] randombuf(int size) {
        Random r = new Random();
        byte[] b = new byte[size];
        r.nextBytes(b);
        return b;
    }

}
