package com.aetherpmo.domain.task.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskUpdateRequest(
        String taskName,
        String status,
        Long assigneeId,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        BigDecimal plannedEffort,
        BigDecimal actualEffort,
        Integer sortOrder,
        String description
) {
}
