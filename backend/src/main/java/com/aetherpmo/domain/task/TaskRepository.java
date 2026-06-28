package com.aetherpmo.domain.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

    List<Task> findByParentTaskIdOrderBySortOrderAscIdAsc(Long parentTaskId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, String status);
}
