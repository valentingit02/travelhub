package com.travelhub.catalogo.auto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BloqueoAutoRepository extends JpaRepository<BloqueoAuto, Long> {

    /** Cantidad de bloqueos del auto que se superponen con el rango pedido. */
    @Query("""
           select count(b) from BloqueoAuto b
           where b.autoId = :autoId and b.desde < :hasta and b.hasta > :desde
           """)
    long contarSuperpuestos(@Param("autoId") Long autoId, @Param("desde") LocalDate desde,
                            @Param("hasta") LocalDate hasta);

    @Modifying
    @Query("delete from BloqueoAuto b where b.reservaRef = :reservaRef")
    int liberarPorReserva(@Param("reservaRef") String reservaRef);
}
