package com.jcraft.jzlib;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GZIPIOStreamTest {

    @Test
    public void deflateAndInflateData() throws IOException {
        var comment = "hi";
        var name = "/tmp/foo";

        var content = "hello".getBytes(StandardCharsets.UTF_8);

        var baos = new ByteArrayOutputStream();
        var gos = new GZIPOutputStream(baos);

        gos.setComment(comment);
        gos.setName(name);

        gos.write(content);
        gos.close();

        var bais = new ByteArrayInputStream(baos.toByteArray());
        var gis = new GZIPInputStream(bais);

        var buf = new byte[1024];
        var i = gis.read(buf);

        assertEquals(content.length, i);
        for (int c = 0; c < i; ++c) {
            assertEquals(content[c], buf[c]);
        }

        assertEquals(comment, gis.getComment());
        assertEquals(name, gis.getName());

        var crc32 = new CRC32();
        crc32.update(content, 0, content.length);

        assertEquals(gis.getCRC(), crc32.getValue());
    }

    // https://github.com/ymnk/jzlib/issues/9
    // https://github.com/jglick/jzlib-9-demo
    @Test
    public void deflateSomeFileWithoutAIOBE() throws Exception {
        var pos = new PipedOutputStream();
        var pis = new PipedInputStream(pos);
        var csOut = new java.util.zip.CRC32();
        var gos = new GZIPOutputStream(pos);
        var cos = new CheckedOutputStream(gos, csOut);

        var t = new Thread(
            () -> {
                try {
                    var fail = GZIPIOStreamTest.class.getResourceAsStream("/jzlib.fail.gz");
                    assertNotNull(fail);

                    var fis = new java.util.zip.GZIPInputStream(fail);
                    fis.transferTo(cos);
                    cos.close();
                }
                catch(IOException x) {
                    throw new UncheckedIOException(x);
                }
            }
        );
        t.start();

        var gis = new GZIPInputStream(pis);
        var csIn = new java.util.zip.CRC32();
        new CheckedInputStream(gis, csIn).transferTo(OutputStream.nullOutputStream());

        t.join();

        assertEquals(csIn.getValue(), csOut.getValue());

    }

}
