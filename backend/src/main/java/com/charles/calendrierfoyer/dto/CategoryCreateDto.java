package com.charles.calendrierfoyer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateDto(
        @NotBlank @Size(max = 60) String name,
        @NotBlank @Size(max = 64) String icon) {}
