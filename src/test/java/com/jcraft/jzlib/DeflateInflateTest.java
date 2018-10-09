package com.jcraft.jzlib;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.jcraft.jzlib.JZlib.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeflateInflateTest {

    private static final int COMPRESSED_LEN = 40000;
    private static final int UNCOMPRESSED_LEN = COMPRESSED_LEN;
    private byte[] compressedBuffer;
    private byte[] uncompressedBuffer;

    private Deflater deflater;
    private Inflater inflater;

    @BeforeEach
    public void before() {
        compressedBuffer = new byte[COMPRESSED_LEN];
        uncompressedBuffer = new byte[UNCOMPRESSED_LEN];
        deflater = new Deflater();
        inflater = new Inflater();
    }

    @Test
    public void deflateInflateInLargeBuffer() {
        var error = deflater.init(Z_BEST_SPEED);
        assertEquals(Z_OK, error);

        deflater.setInput(uncompressedBuffer);
        deflater.setOutput(compressedBuffer);

        error = deflater.deflate(Z_NO_FLUSH);
        assertEquals(Z_OK, error);

        assertEquals(0, deflater.avail_in);

        deflater.params(Z_NO_COMPRESSION, Z_DEFAULT_STRATEGY);
        deflater.setInput(compressedBuffer);
        deflater.avail_in = COMPRESSED_LEN / 2;

        error = deflater.deflate(Z_NO_FLUSH);
        assertEquals(Z_OK, error);

        deflater.params(Z_BEST_COMPRESSION, Z_FILTERED);
        deflater.setInput(uncompressedBuffer);
        deflater.avail_in = UNCOMPRESSED_LEN;

        error = deflater.deflate(Z_NO_FLUSH);
        assertEquals(Z_OK, error);

        error = deflater.deflate(Z_FINISH);
        assertEquals(Z_STREAM_END, error);

        error = deflater.end();
        assertEquals(Z_OK, error);

        inflater.setInput(compressedBuffer);

        error = inflater.init();
        assertEquals(Z_OK, error);

        while (true) {
            inflater.setOutput(uncompressedBuffer);
            error = inflater.inflate(Z_NO_FLUSH);
            if (error == Z_STREAM_END) {
                break;
            }
            assertEquals(Z_OK, error);
        }

        error = inflater.end();
        assertEquals(Z_OK, error);

        assertEquals(2 * UNCOMPRESSED_LEN + COMPRESSED_LEN / 2, inflater.total_out);
    }

    @Test
    public void deflateInflateInSmallBuffer() {
        var data = "hello, hello!".getBytes(StandardCharsets.UTF_8);

        var error = deflater.init(Z_DEFAULT_COMPRESSION);
        assertEquals(Z_OK, error);

        deflater.setInput(data);
        deflater.setOutput(compressedBuffer);

        while (deflater.total_in < data.length &&
            deflater.total_out < COMPRESSED_LEN) {
            deflater.avail_in = 1;
            deflater.avail_out = 1;
            error = deflater.deflate(Z_NO_FLUSH);
            assertEquals(Z_OK, error);
        }

        do {
            deflater.avail_out = 1;
            error = deflater.deflate(Z_FINISH);
        }
        while (error != Z_STREAM_END);

        error = deflater.end();
        assertEquals(Z_OK, error);

        inflater.setInput(compressedBuffer);
        inflater.setOutput(uncompressedBuffer);

        error = inflater.init();
        assertEquals(Z_OK, error);

        while (inflater.total_out < UNCOMPRESSED_LEN &&
            inflater.total_in < COMPRESSED_LEN) {
            inflater.avail_in = 1; // force small buffers
            inflater.avail_out = 1; // force small buffers
            error = inflater.inflate(Z_NO_FLUSH);
            if (error == Z_STREAM_END) {
                break;
            }
            assertEquals(Z_OK, error);
        }

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut = (int) inflater.total_out;
        var actual = new byte[totalOut];
        System.arraycopy(uncompressedBuffer, 0, actual, 0, totalOut);

        assertArrayEquals(data, actual);
    }

    @Test
    public void supportDictionary() {
        var hello = "hello".getBytes(StandardCharsets.UTF_8);
        var dictionary = "hello, hello!".getBytes(StandardCharsets.UTF_8);

        var error = deflater.init(Z_DEFAULT_COMPRESSION);
        assertEquals(Z_OK, error);

        deflater.setDictionary(dictionary, dictionary.length);
        assertEquals(Z_OK, error);

        var dictID = deflater.getAdler();

        deflater.setInput(hello);
        deflater.setOutput(compressedBuffer);

        error = deflater.deflate(Z_FINISH);
        assertEquals(Z_STREAM_END, error);

        error = deflater.end();
        assertEquals(Z_OK, error);

        error = inflater.init();
        assertEquals(Z_OK, error);

        inflater.setInput(compressedBuffer);
        inflater.setOutput(uncompressedBuffer);

        var loop = true;
        do {
            error = inflater.inflate(JZlib.Z_NO_FLUSH);
            switch (error) {
                case Z_STREAM_END:
                    loop = false;
                    break;
                case Z_NEED_DICT:
                    assertEquals(inflater.getAdler(), dictID);
                    error = inflater.setDictionary(dictionary, dictionary.length);
                    assertEquals(Z_OK, error);
                    break;
                default:
                    assertEquals(Z_OK, error);
                    break;
            }
        }
        while (loop);

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut = (int) inflater.total_out;
        var actual = new byte[totalOut];
        System.arraycopy(uncompressedBuffer, 0, actual, 0, totalOut);

        assertArrayEquals(hello, actual);
    }

    @Test
    public void supportSync() {
        var hello = "hello".getBytes(StandardCharsets.UTF_8);

        var error = deflater.init(Z_DEFAULT_COMPRESSION);
        assertEquals(Z_OK, error);

        deflater.setInput(hello);
        deflater.avail_in = 3;
        deflater.setOutput(compressedBuffer);

        error = deflater.deflate(Z_FULL_FLUSH);
        assertEquals(Z_OK, error);

        compressedBuffer[3] = (byte) (compressedBuffer[3] + 1);
        deflater.avail_in = hello.length - 3;

        error = deflater.deflate(Z_FINISH);
        assertEquals(Z_STREAM_END, error);
        int compressedLength = (int) deflater.total_out;

        error = deflater.end();
        assertEquals(Z_OK, error);

        error = inflater.init();
        assertEquals(Z_OK, error);

        inflater.setInput(compressedBuffer);
        inflater.avail_in = 2;

        inflater.setOutput(uncompressedBuffer);

        error = inflater.inflate(JZlib.Z_NO_FLUSH);
        assertEquals(Z_OK, error);

        inflater.avail_in = compressedLength - 2;
        error = inflater.sync();

        error = inflater.inflate(Z_FINISH);
        assertEquals(Z_DATA_ERROR, error);

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut = (int) inflater.total_out;
        var actual = new byte[totalOut];
        System.arraycopy(uncompressedBuffer, 0, actual, 0, totalOut);

        assertEquals("hel" + new String(actual, StandardCharsets.UTF_8),
            new String(hello, StandardCharsets.UTF_8));
    }

    @Test
    public void inflateGzipData() {
        var hello = "foo".getBytes(StandardCharsets.UTF_8);
        var data = new byte[]{0x1f, (byte) 0x8b, 0x08, 0x18, 0x08, (byte) 0xeb,
            0x7a, 0x0b, 0x00, 0x0b, 0x58, 0x00, 0x59, 0x00, 0x4b, (byte) 0xcb,
            (byte) 0xcf, 0x07, 0x00, 0x21, 0x65, 0x73, (byte) 0x8c, 0x03, 0x00,
            0x00, 0x00};

        var error = inflater.init(15 + 32);
        assertEquals(Z_OK, error);

        inflater.setInput(data);
        inflater.setOutput(uncompressedBuffer);

        var compressedLength = data.length;

        while (inflater.total_out < UNCOMPRESSED_LEN &&
            inflater.total_in < compressedLength) {

            error = inflater.inflate(Z_NO_FLUSH);
            if (error == Z_STREAM_END) {
                break;
            }
            assertEquals(Z_OK, error);
        }

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut = (int) inflater.total_out;
        var actual = new byte[totalOut];
        System.arraycopy(uncompressedBuffer, 0, actual, 0, totalOut);

        assertArrayEquals(hello, actual);
    }

    @Test
    public void supportGZipData() {
        var data = "hello, hello!".getBytes(StandardCharsets.UTF_8);

        var error = deflater.init(Z_DEFAULT_COMPRESSION, 15 + 16);
        assertEquals(Z_OK, error);

        deflater.setInput(data);
        deflater.setOutput(compressedBuffer);

        while (deflater.total_in < data.length &&
            deflater.total_out < COMPRESSED_LEN) {
            deflater.avail_in = 1;
            deflater.avail_out = 1;
            error = deflater.deflate(Z_NO_FLUSH);
            assertEquals(Z_OK, error);
        }

        do {
            deflater.avail_out = 1;
            error = deflater.deflate(Z_FINISH);
        }
        while (error != Z_STREAM_END);

        error = deflater.end();
        assertEquals(Z_OK, error);

        inflater.setInput(compressedBuffer);
        inflater.setOutput(uncompressedBuffer);

        error = inflater.init(15 + 32);
        assertEquals(Z_OK, error);

        while (inflater.total_out < UNCOMPRESSED_LEN &&
            inflater.total_in < COMPRESSED_LEN) {
            inflater.avail_in = 1; // force small buffers
            inflater.avail_out = 1; // force small buffers

            error = inflater.inflate(Z_NO_FLUSH);
            if (error == Z_STREAM_END) {
                break;
            }
            assertEquals(Z_OK, error);
        }

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut = (int) inflater.total_out;
        var actual = new byte[totalOut];
        System.arraycopy(uncompressedBuffer, 0, actual, 0, totalOut);

        assertArrayEquals(data, actual);
    }
}
