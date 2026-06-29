package com.aetherpmo.domain.task.dto;

import com.aetherpmo.domain.task.Task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record TaskTreeDto(
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
        String description,
        Long catalogNodeId,
        Long workflowId,
        List<TaskTreeDto> children
) {
    public static TaskTreeDto from(Task t) {
        return from(t, null);
    }

    public static TaskTreeDto from(Task t, Long workflowId) {
        return new TaskTreeDto(
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
                t.getDescription(),
                t.getCatalogNodeId(),
                workflowId,
                new ArrayList<>()
        );
    }
}
