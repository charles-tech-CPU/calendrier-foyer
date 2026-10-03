package com.charles.calendrierfoyer.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Une occurrence concrete affichee sur le calendrier (une date precise). */
public record EventOccurrenceDto(
        Long eventId,
        String title,
        String description,
        String location,
        String color,
        boolean allDay,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean recurring) {}
