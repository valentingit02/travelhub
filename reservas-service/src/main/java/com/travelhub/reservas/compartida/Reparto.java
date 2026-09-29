package com.travelhub.reservas.compartida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Divide un monto en N partes con centavos exactos: el organizador (parte 0) absorbe el resto. */
public final class Reparto {

    private Reparto() { }

    public static List<BigDecimal> partes(BigDecimal total, int personas) {
        if (personas < 1) throw new IllegalArgumentException("Tiene que haber al menos una persona");
        BigDecimal parte = total.divide(BigDecimal.valueOf(personas), 2, RoundingMode.DOWN);
        BigDecimal resto = total.subtract(parte.multiply(BigDecimal.valueOf(personas)));
        List<BigDecimal> out = new ArrayList<>();
        for (int i = 0; i < personas; i++) out.add(i == 0 ? parte.add(resto) : parte);
        return out;
    }
}
