package com.aetherpmo.domain.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    Optional<Workflow> findFirstByIsDefaultTrue();
}
