package com.aetherpmo.domain.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, Long> {

    List<WorkflowStatus> findByWorkflowIdOrderBySortOrder(Long workflowId);
}
