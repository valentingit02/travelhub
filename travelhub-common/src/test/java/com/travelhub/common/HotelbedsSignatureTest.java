package com.travelhub.common;

import com.travelhub.common.util.HotelbedsSignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HotelbedsSignatureTest {

    @Test
    void firmaEsSha256HexDe64Caracteres() {
        String f = HotelbedsSignature.firmar("key", "secret", 1700000000L);
        assertEquals(64, f.length());
        assertEquals(f, HotelbedsSignature.firmar("key", "secret", 1700000000L));
    }
}
