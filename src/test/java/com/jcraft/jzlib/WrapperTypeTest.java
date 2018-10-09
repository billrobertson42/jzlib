package com.jcraft.jzlib;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static com.jcraft.jzlib.JZlib.*;

class WrapperTypeTest {


    public static List<Arguments> goodFlagCombos() throws IOException {
        return List.of(
            Arguments.of(W_ZLIB, W_ZLIB),
            Arguments.of(W_ZLIB, W_ANY),
            Arguments.of(W_GZIP, W_GZIP),
            Arguments.of(W_GZIP, W_ANY),
            Arguments.of(W_NONE, W_NONE),
            Arguments.of(W_NONE, W_ANY));
    }

    public static List<Arguments> badFlagCombos() throws IOException {
        return List.of(
            Arguments.of(W_ZLIB, W_GZIP),
            Arguments.of(W_ZLIB, W_NONE),
            Arguments.of(W_GZIP, W_ZLIB),
            Arguments.of(W_GZIP, W_NONE),
            Arguments.of(W_NONE, W_GZIP),
            Arguments.of(W_NONE, W_ZLIB));
    }

    @ParameterizedTest
    @MethodSource("goodFlagCombos")
    public void testGoodDeflaterFlagCombinations(WrapperType iflag, WrapperType good) throws IOException {
        byte[] data = "hello! hello!".getBytes(StandardCharsets.UTF_8);
        byte[] deflated = deflate(data, iflag);
        var baos2 = new ByteArrayOutputStream();
        var inflater = new Inflater(good);
        new InflaterInputStream(new ByteArrayInputStream(deflated), inflater).transferTo(baos2);
        var data1 = baos2.toByteArray();
        assertArrayEquals(data, data1, "testing that " + iflag + " works with " + good);
    }

    @ParameterizedTest
    @MethodSource("badFlagCombos")
    public void testBadDeflaterFlagCombinations(WrapperType iflag, WrapperType bad) throws IOException {
        byte[] deflated = deflate("good bye good bye".getBytes(StandardCharsets.UTF_8), iflag);
        assertThrows(IOException.class, () -> {
            var baos2 = new ByteArrayOutputStream();
            var inflater = new Inflater(bad);
            new InflaterInputStream(new ByteArrayInputStream(deflated), inflater).transferTo(baos2);
        }, "testing that " + iflag + " does not work with " + bad);
    }

    @ParameterizedTest
    @MethodSource("goodFlagCombos")
    public void testGoodZStreamFlagCombinations(WrapperType iflag, WrapperType good) throws IOException {
        var deflater = new ZStream();
        var error = deflater.deflateInit(Z_BEST_SPEED, DEF_WBITS, 9, iflag);
        assertEquals(Z_OK, error);

        var expected = "hello! hello!".getBytes(StandardCharsets.UTF_8);
        var compressed = new byte[200];
        deflate(deflater, expected, compressed);

        var inflater = new ZStream();
        var uncompressed = new byte[compressed.length];
        error = inflater.inflateInit(good);
        assertEquals(Z_OK, error);
        inflate(inflater, compressed, uncompressed);
        assertEquals(expected.length, inflater.total_out);
        var actual = new byte[(int)inflater.total_out];
        System.arraycopy(uncompressed, 0, actual, 0, actual.length);
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("badFlagCombos")
    public void testBadZStreamFlagCombinations(WrapperType iflag, WrapperType bad) throws IOException {
        var deflater = new ZStream();
        var error = deflater.deflateInit(Z_BEST_SPEED, DEF_WBITS, 9, iflag);
        assertEquals(Z_OK, error);

        var compressed = new byte[200];
        deflate(deflater, "hello! hello!".getBytes(StandardCharsets.UTF_8), compressed);

        var uncompressed = new byte[200];
        var inflater = new ZStream();
        error = inflater.inflateInit(bad);
        assertEquals(Z_OK, error);
        inflater.setInput(compressed);

        inflater.setOutput(uncompressed);
        error = inflater.inflate(Z_NO_FLUSH);
        assertEquals(Z_DATA_ERROR, error);
    }

    @Test
    public void deflaterShouldSupportWBitsPlus32() {
        var deflater = new Deflater();
        var error = deflater.init(Z_BEST_SPEED, DEF_WBITS, 9);
        assertEquals(Z_OK, error);

        var expected = "hello! hello!".getBytes(StandardCharsets.UTF_8);
        var compressed = new byte[200];
        deflate(deflater, expected, compressed);

        var inflater = new Inflater();
        error = inflater.init(DEF_WBITS + 32);
        assertEquals(Z_OK, error);

        inflater.setInput(compressed);
        var uncompressed = new byte[200];

        while(true) {
            inflater.setOutput(uncompressed);
            error = inflater.inflate(Z_NO_FLUSH);
            if(error == Z_STREAM_END) {
                break;
            }
            assertEquals(Z_OK, error);
        }

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut = (int)inflater.total_out;
        assertEquals(expected.length, totalOut);
        var actual = new byte[totalOut];
        System.arraycopy(uncompressed, 0, actual, 0, actual.length);
        assertArrayEquals(expected, actual);

        deflater = new Deflater();
        error = deflater.init(Z_BEST_SPEED, DEF_WBITS + 16, 9);
        assertEquals(Z_OK, error);

        deflate(deflater, expected, compressed);

        inflater = new Inflater();
        error = inflater.init(DEF_WBITS + 32);
        assertEquals(Z_OK, error);

        inflater.setInput(compressed);


        while(true) {
            inflater.setOutput(uncompressed);
            error = inflater.inflate(Z_NO_FLUSH);
            if(error == Z_STREAM_END) {
                break;
            }
            assertEquals(Z_OK, error);
        }

        error = inflater.end();
        assertEquals(Z_OK, error);

        int totalOut2 = (int)inflater.total_out;
        assertEquals(expected.length, totalOut2);
        var actual2 = new byte[totalOut];
        System.arraycopy(uncompressed, 0, actual2, 0, actual.length);
        assertArrayEquals(expected, actual2);

    }


    private byte[] deflate(byte[] data, WrapperType iflag) throws IOException {
        var baos = new ByteArrayOutputStream();
        var deflater = new Deflater(Z_DEFAULT_COMPRESSION, DEF_WBITS, 9, iflag);
        var gos = new DeflaterOutputStream(baos, deflater);
        gos.write(data);
        gos.close();
        return baos.toByteArray();
    }

    private void deflate(ZStream deflater, byte[] data, byte[] compr) {
        deflater.setInput(data);
        deflater.setOutput(compr);

        var err = deflater.deflate(JZlib.Z_FINISH);
        assertEquals(Z_STREAM_END, err);

        err = deflater.end();
        assertEquals(Z_OK, err);
    }

    private void inflate(ZStream inflater, byte[] source, byte[] dest) {
        inflater.setInput(source);
        var loop = true;
        int error;
        while(loop) {
            inflater.setOutput(dest);
            error = inflater.inflate(Z_NO_FLUSH);
            if(error == Z_STREAM_END) loop = false;
            else assertEquals(Z_OK, error);
        }
        error = inflater.end();
        assertEquals(Z_OK, error);
    }

}
