package com.charles.calendrierfoyer.repository;

import com.charles.calendrierfoyer.domain.Event;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Tous les evenements potentiellement visibles sur [from, to] :
     * - les ponctuels dont la date (ou la periode startDate..endDate) chevauche la plage ;
     * - les recurrents dont la serie a demarre avant la fin de la plage et dont la
     *   derniere occurrence a commence au plus tot le earliestStart (= from moins la
     *   duree maximale d'une periode, pour ne pas rater une periode deja commencee).
     *   Filtrage large : l'expansion exacte se fait ensuite en Java via RecurrenceService.
     */
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.person LEFT JOIN FETCH e.category WHERE "
            + "(e.recurrenceFrequency = com.charles.calendrierfoyer.domain.RecurrenceFrequency.NONE "
            + "   AND e.startDate <= :to AND COALESCE(e.endDate, e.startDate) >= :from) "
            + "OR (e.recurrenceFrequency <> com.charles.calendrierfoyer.domain.RecurrenceFrequency.NONE "
            + "   AND e.startDate <= :to "
            + "   AND (e.recurrenceEndDate IS NULL OR e.recurrenceEndDate >= :earliestStart))")
    List<Event> findRelevantForRange(
            @Param("from") LocalDate from, @Param("to") LocalDate to, @Param("earliestStart") LocalDate earliestStart);

    /**
     * Avant de supprimer une personne : ses evenements restent, sans personne,
     * mais gardent la couleur qu'ils avaient a l'ecran.
     */
    @Modifying
    @Query("UPDATE Event e SET e.person = null, e.color = :color WHERE e.person.id = :personId")
    void detachPerson(@Param("personId") Long personId, @Param("color") String color);
}
