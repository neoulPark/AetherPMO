package com.aetherpmo.domain.task.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskCreateRequest(
        Long parentTaskId,
        @NotBlank String taskName,
        String status,
        Integer progressRate,
        Long assigneeId,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        BigDecimal plannedEffort,
        Integer sortOrder,
        String description
) {
}
