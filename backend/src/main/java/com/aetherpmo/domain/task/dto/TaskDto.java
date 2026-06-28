package com.aetherpmo.domain.task.dto;

import com.aetherpmo.domain.task.Task;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskDto(
        Long id,
        Long parentTaskId,
        Long projectId,
        String taskName,
        String status,
        Integer progressRate,
        Long assigneeId,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        BigDecimal plannedEffort,
        BigDecimal actualEffort,
        Integer depth,
        Integer sortOrder,
        String description
) {
    public static TaskDto from(Task t) {
        return new TaskDto(
                t.getId(),
                t.getParentTaskId(),
                t.getProjectId(),
                t.getTaskName(),
                t.getStatus(),
                t.getProgressRate(),
                t.getAssigneeId(),
                t.getPlannedStartDate(),
                t.getPlannedEndDate(),
                t.getActualStartDate(),
                t.getActualEndDate(),
                t.getPlannedEffort(),
                t.getActualEffort(),
                t.getDepth(),
                t.getSortOrder(),
                t.getDescription()
        );
    }
}
