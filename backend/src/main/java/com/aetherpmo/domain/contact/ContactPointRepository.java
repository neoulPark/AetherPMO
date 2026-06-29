package com.aetherpmo.domain.contact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactPointRepository extends JpaRepository<ContactPoint, Long> {

    List<ContactPoint> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);
}
