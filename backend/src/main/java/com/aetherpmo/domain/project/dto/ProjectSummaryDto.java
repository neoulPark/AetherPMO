package com.aetherpmo.domain.project.dto;

public record ProjectSummaryDto(
        Long projectId,
        String projectName,
        Integer progressRate,
        long totalTasks,
        long todoTasks,
        long inProgressTasks,
        long reviewTasks,
        long doneTasks
) {
}
