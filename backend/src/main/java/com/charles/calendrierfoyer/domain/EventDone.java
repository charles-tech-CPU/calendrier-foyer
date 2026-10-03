package com.charles.calendrierfoyer.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Une occurrence precise d'un evenement cochee "c'est fait". */
@Entity
@Table(name = "event_done")
@IdClass(EventDone.Key.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventDone {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Id
    @Column(name = "occurrence_date")
    private LocalDate occurrenceDate;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long eventId;
        private LocalDate occurrenceDate;
    }
}
