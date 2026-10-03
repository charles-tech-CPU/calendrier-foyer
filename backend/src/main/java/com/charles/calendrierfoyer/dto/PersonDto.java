package com.charles.calendrierfoyer.dto;

import java.time.LocalDate;

public record PersonDto(
        Long id, String firstName, String lastName, LocalDate birthDate, String color, boolean hasPhoto) {}
