package com.aetherpmo.domain.methodology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliverableTemplateRepository extends JpaRepository<DeliverableTemplate, Long> {

    List<DeliverableTemplate> findByTaskTemplateIdOrderBySeqNoAscIdAsc(Long taskTemplateId);

    List<DeliverableTemplate> findByIdInOrderBySeqNoAscIdAsc(List<Long> ids);
}
