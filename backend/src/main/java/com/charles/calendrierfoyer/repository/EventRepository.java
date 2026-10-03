package com.charles.calendrierfoyer.repository;

import com.charles.calendrierfoyer.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Tous les evenements potentiellement visibles sur [from, to] :
     * - les ponctuels dont la date tombe dans la plage (filtrage exact en SQL) ;
     * - les recurrents dont la serie a demarre avant la fin de la plage et n'est
     *   pas deja terminee avant le debut de la plage (filtrage large ; l'expansion
     *   exacte des occurrences se fait ensuite en Java via RecurrenceService).
     */
    @Query("SELECT e FROM Event e WHERE " +
            "(e.recurrenceFrequency = com.charles.calendrierfoyer.domain.RecurrenceFrequency.NONE " +
            "   AND e.startDate BETWEEN :from AND :to) " +
            "OR (e.recurrenceFrequency <> com.charles.calendrierfoyer.domain.RecurrenceFrequency.NONE " +
            "   AND e.startDate <= :to " +
            "   AND (e.recurrenceEndDate IS NULL OR e.recurrenceEndDate >= :from))")
    List<Event> findRelevantForRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
