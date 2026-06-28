package com.aetherpmo.domain.methodology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {

    List<TaskTemplate> findByActivityIdOrderBySortOrderAscIdAsc(Long activityId);

    List<TaskTemplate> findByIdInOrderBySortOrderAscIdAsc(List<Long> ids);
}
