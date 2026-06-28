package com.aetherpmo.domain.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskAssignmentHistoryRepository extends JpaRepository<TaskAssignmentHistory, Long> {

    List<TaskAssignmentHistory> findByTaskIdOrderByChangedAtDesc(Long taskId);
}
