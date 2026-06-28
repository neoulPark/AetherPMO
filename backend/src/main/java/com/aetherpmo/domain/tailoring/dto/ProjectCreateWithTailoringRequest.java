package com.aetherpmo.domain.tailoring.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProjectCreateWithTailoringRequest(
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
        String riskLevel,
        String team,
        String location,
        String businessType,
        List<Long> selectedTaskTemplateIds,
        List<Long> selectedDeliverableTemplateIds
) {
}
