package com.charles.calendrierfoyer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Un evenement du calendrier unique et partage du foyer. Peut etre ponctuel
 * (recurrenceFrequency = NONE, une seule occurrence a startDate) ou recurrent
 * (les occurrences suivantes ne sont jamais stockees : elles sont recalculees
 * a la volee par RecurrenceService pour la plage de dates demandee - meme
 * principe que le classement tennis ou les totaux budget, on ne materialise
 * jamais ce qui peut etre recalcule).
 * v1 : modifier ou supprimer un evenement recurrent agit sur toute la serie
 * (pas d'exception ponctuelle sur une seule occurrence - voir README).
 */
@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(length = 160)
    private String location;

    /** Couleur libre au format hexadecimal (ex: "#4a90d9"), optionnelle. */
    @Column(length = 20)
    private String color;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    /** Date de la premiere (ou unique) occurrence. */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Nul si allDay = true. */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", nullable = false, length = 10)
    private RecurrenceFrequency recurrenceFrequency = RecurrenceFrequency.NONE;

    /** Ex: 2 avec WEEKLY = "toutes les 2 semaines". Toujours >= 1. */
    @Column(name = "recurrence_interval", nullable = false)
    private int recurrenceInterval = 1;

    /** Uniquement pour WEEKLY : jours concernes, ex "MON,WED,FRI". Null = jour de startDate. */
    @Column(name = "recurrence_days_of_week", length = 30)
    private String recurrenceDaysOfWeek;

    /** Null = la serie ne s'arrete jamais. */
    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;
}
