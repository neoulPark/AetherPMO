package com.aetherpmo.domain.deliverable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEntityTypeAndEntityIdOrderBySortOrderAscIdAsc(String entityType, Long entityId);

    long countByEntityTypeAndEntityId(String entityType, Long entityId);
}
