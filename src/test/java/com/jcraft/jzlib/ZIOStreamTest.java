package com.jcraft.jzlib;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

import static com.jcraft.jzlib.JZlib.*;

class ZIOStreamTest {

    @Test
    public void inflateAndDeflateData() throws Exception {
        var hello = "Hello World!";

        var out = new ByteArrayOutputStream();
        var zOut = new ZOutputStream(out, Z_BEST_COMPRESSION);
        var objOut = new ObjectOutputStream(zOut);
        objOut.writeObject(hello);
        zOut.close();

        var in = new ByteArrayInputStream(out.toByteArray());
        var zIn = new ZInputStream(in);
        var objIn = new ObjectInputStream(zIn);

        assertEquals(hello, objIn.readObject());
    }

    @Test
    public void supportNoWrapData() throws IOException {
        var buf = new byte[100];
        var hello = "Hello World!".getBytes(StandardCharsets.UTF_8);

        var baos = new ByteArrayOutputStream();
        var zos = new ZOutputStream(baos, Z_DEFAULT_COMPRESSION, true);
        zos.write(hello);
        zos.close();

        var baos2 = new ByteArrayOutputStream();
        var zis = new ZInputStream(new ByteArrayInputStream(baos.toByteArray()), true);
        zis.transferTo(baos2);
        var data2 = baos2.toByteArray();

        assertArrayEquals(hello, data2);
    }

}
