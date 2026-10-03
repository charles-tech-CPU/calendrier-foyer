package com.charles.calendrierfoyer.dto;

import com.charles.calendrierfoyer.domain.RecurrenceFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record EventCreateDto(
        @NotBlank String title,
        String description,
        String location,
        String color,
        @NotNull Boolean allDay,
        @NotNull LocalDate startDate,
        LocalTime startTime,
        LocalTime endTime,
        RecurrenceFrequency recurrenceFrequency,
        Integer recurrenceInterval,
        String recurrenceDaysOfWeek,
        LocalDate recurrenceEndDate) {}
