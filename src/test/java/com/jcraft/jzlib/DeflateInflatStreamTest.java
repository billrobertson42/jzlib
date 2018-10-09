package com.jcraft.jzlib;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.jcraft.jzlib.JZlib.DEF_WBITS;
import static com.jcraft.jzlib.JZlib.Z_DEFAULT_COMPRESSION;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DeflateInflatStreamTest {

    @Test
    public void deflateInflateOneByOne() throws IOException {
        var data1 = TestUtil.randombuf(1024);

        var baos = new ByteArrayOutputStream();
        var gos = new DeflaterOutputStream(baos);

        gos.write(data1);
        gos.close();

        var baos2 = new ByteArrayOutputStream();
        new InflaterInputStream(new ByteArrayInputStream(baos.toByteArray())).transferTo(baos2);

        var data2 = baos2.toByteArray();
        assertArrayEquals(data1, data2);
    }

    @Test
    public void deflateAndInflate() throws IOException {
        for (int i = 1; i <= 100; i += 3) {
            var data1 = TestUtil.randombuf(10240);

            var baos = new ByteArrayOutputStream();
            var gos = new DeflaterOutputStream(baos);
            gos.write(data1);
            gos.close();

            var baos2 = new ByteArrayOutputStream();
            new InflaterInputStream(new ByteArrayInputStream(baos.toByteArray())).transferTo(baos2);
            var data2 = baos2.toByteArray();

            assertArrayEquals(data1, data2);
        }
    }

    @Test
    public void deflateAndInflateNowrapData() throws IOException {
        for (int i = 1; i <= 100; i += 3) {
            var data1 = TestUtil.randombuf(10240);

            var baos = new ByteArrayOutputStream();
            var deflater = new Deflater(Z_DEFAULT_COMPRESSION, DEF_WBITS, true);
            var gos = new DeflaterOutputStream(baos, deflater);
            gos.write(data1);
            gos.close();

            var baos2 = new ByteArrayOutputStream();
            var inflater = new Inflater(DEF_WBITS, true);
            new InflaterInputStream(new ByteArrayInputStream(baos.toByteArray()), inflater).transferTo(baos2);
            var data2 = baos2.toByteArray();

            assertArrayEquals(data1, data2);
        }
    }

    @Test
    public void deflateAndInflateNowrapDataWithMAXWBITS() throws IOException {

        byte[][] buffers = {
            TestUtil.randombuf(10240),
            "{\"color\":2,\"id\":\"EvLd4UG.CXjnk35o1e8LrYYQfHu0h.d*SqVJPoqmzXM::Ly::Snaps::Store::Commit\"}".getBytes(StandardCharsets.UTF_8)
        };

        for (byte[] data1 : buffers) {

            var deflater = new Deflater(JZlib.Z_DEFAULT_COMPRESSION,
                JZlib.MAX_WBITS,
                true);

            var inflater = new Inflater(JZlib.MAX_WBITS, true);

            var baos = new ByteArrayOutputStream();
            var gos = new DeflaterOutputStream(baos, deflater);
            gos.write(data1);
            gos.close();

            var baos2 = new ByteArrayOutputStream();
            new InflaterInputStream(new ByteArrayInputStream(baos.toByteArray()), inflater).transferTo(baos2);

            var data2 = baos2.toByteArray();

            assertArrayEquals(data1, data2);
        }
    }
}
