package com.aetherpmo.domain.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectUpdateRequest(
        String projectName,
        String projectCode,
        String description,
        Long pmId,
        Long clientCompanyId,
        String status,
        String projectStage,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        BigDecimal contractAmount,
        Integer progressRate,
        String riskLevel,
        String team,
        String location,
        String businessType
) {
}
