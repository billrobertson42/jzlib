package com.jcraft.jzlib;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Adler32Test {

    private Adler32 subject;

    @BeforeEach
    public void create() {
        subject = new Adler32();
    }

    @Test
    public void compatiblity() {
        var buf1 = TestUtil.randombuf(1024);
        var buf2 = new byte[buf1.length];
        System.arraycopy(buf1, 0, buf2, 0, buf2.length);
        assertArrayEquals(buf1, buf2);

        var juza = new java.util.zip.Adler32();
        juza.update(buf2, 0, buf2.length);
        long expected = juza.getValue();

        subject.update(buf1, 0, buf1.length);
        long actual = subject.getValue();

        assertEquals(expected, actual);
    }

    @Test
    public void copy() {
        var buf1 = TestUtil.randombuf(1024);
        var buf2 = TestUtil.randombuf(1024);

        subject.update(buf1, 0, buf1.length);

        var subject2 = subject.copy();

        subject.update(buf2, 0, buf1.length);
        subject2.update(buf2, 0, buf1.length);

        var expected = subject.getValue();
        var actual = subject2.getValue();

        assertEquals(expected, actual);
    }

    @Test
    public void combine() {
        var buf1 = TestUtil.randombuf(1024);
        var buf2 = TestUtil.randombuf(1024);

        var adler1 = getValue(subject, buf1);
        var adler2 = getValue(subject, buf2);
        var expected = getValue(subject, buf1, buf2);
        var actual = Adler32.combine(adler1, adler2, buf2.length);
        assertEquals(expected, actual);
    }

    private long getValue(Adler32 adler, byte[]... buffers) {
        adler.reset();
        for (byte[] buffer : buffers) {
            adler.update(buffer, 0, buffer.length);
        }
        return adler.getValue();
    }


}
