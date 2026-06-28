package com.aetherpmo.domain.methodology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MethodologyActivityRepository extends JpaRepository<MethodologyActivity, Long> {

    List<MethodologyActivity> findByPhaseIdOrderBySortOrderAscIdAsc(Long phaseId);

    List<MethodologyActivity> findAllByOrderBySortOrderAscIdAsc();
}
