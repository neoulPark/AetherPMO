package com.aetherpmo.domain.methodology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MethodologyPhaseRepository extends JpaRepository<MethodologyPhase, Long> {

    List<MethodologyPhase> findAllByOrderBySortOrderAscIdAsc();
}
