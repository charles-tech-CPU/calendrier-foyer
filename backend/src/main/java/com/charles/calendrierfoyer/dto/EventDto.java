package com.charles.calendrierfoyer.dto;

import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import java.time.LocalDate;
import java.time.LocalTime;

/** Definition complete d'un evenement/serie, pour l'ecran d'edition. */
public record EventDto(
        Long id,
        String title,
        String description,
        String location,
        String color,
        boolean allDay,
        LocalDate startDate,
        LocalTime startTime,
        LocalTime endTime,
        RecurrenceFrequency recurrenceFrequency,
        int recurrenceInterval,
        String recurrenceDaysOfWeek,
        LocalDate recurrenceEndDate) {}
