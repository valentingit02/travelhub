package com.travelhub.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Componente de utilidad: firma X-Signature = SHA256(apiKey + secret + epochSegundos) en hexadecimal. */
public final class HotelbedsSignature {

    private HotelbedsSignature() { }

    public static String firmar(String apiKey, String secret, long epochSegundos) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((apiKey + secret + epochSegundos).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public static String firmarAhora(String apiKey, String secret) {
        return firmar(apiKey, secret, System.currentTimeMillis() / 1000);
    }
}
