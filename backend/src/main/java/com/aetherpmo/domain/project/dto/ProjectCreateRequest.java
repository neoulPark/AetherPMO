package com.aetherpmo.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectCreateRequest(
        @NotBlank String projectName,
        String projectCode,
        String description,
        Long pmId,
        Long clientCompanyId,
        String status,
        String projectStage,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        BigDecimal contractAmount,
        String team,
        String location,
        String businessType
) {
}
