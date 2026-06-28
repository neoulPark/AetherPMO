package com.aetherpmo.domain.deliverable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliverableRepository extends JpaRepository<Deliverable, Long> {

    List<Deliverable> findByProjectIdOrderByIdAsc(Long projectId);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatus(Long projectId, String status);
}
