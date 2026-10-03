package com.charles.calendrierfoyer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PersonCreateDto(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotNull @PastOrPresent LocalDate birthDate,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color) {}
