package com.aetherpmo.domain.tailoring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTailoringRepository extends JpaRepository<ProjectTailoring, Long> {

    List<ProjectTailoring> findByProjectIdOrderByIdAsc(Long projectId);
}
