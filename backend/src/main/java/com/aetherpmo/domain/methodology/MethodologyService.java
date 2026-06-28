package com.aetherpmo.domain.methodology;

import com.aetherpmo.domain.methodology.dto.CatalogActivityDto;
import com.aetherpmo.domain.methodology.dto.CatalogDeliverableDto;
import com.aetherpmo.domain.methodology.dto.CatalogPhaseDto;
import com.aetherpmo.domain.methodology.dto.CatalogTaskDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MethodologyService {

    private final MethodologyPhaseRepository phaseRepository;
    private final MethodologyActivityRepository activityRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final DeliverableTemplateRepository deliverableTemplateRepository;

    @Transactional(readOnly = true)
    public List<CatalogPhaseDto> getCatalog() {
        return phaseRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(this::toPhaseDto)
                .toList();
    }

    private CatalogPhaseDto toPhaseDto(MethodologyPhase phase) {
        List<CatalogActivityDto> activities =
                activityRepository.findByPhaseIdOrderBySortOrderAscIdAsc(phase.getId()).stream()
                        .map(this::toActivityDto)
                        .toList();
        return new CatalogPhaseDto(phase.getPhaseCode(), phase.getPhaseName(), activities);
    }

    private CatalogActivityDto toActivityDto(MethodologyActivity activity) {
        List<CatalogTaskDto> tasks =
                taskTemplateRepository.findByActivityIdOrderBySortOrderAscIdAsc(activity.getId()).stream()
                        .map(this::toTaskDto)
                        .toList();
        return new CatalogActivityDto(activity.getActivityCode(), activity.getActivityName(), tasks);
    }

    private CatalogTaskDto toTaskDto(TaskTemplate task) {
        List<CatalogDeliverableDto> deliverables =
                deliverableTemplateRepository.findByTaskTemplateIdOrderBySeqNoAscIdAsc(task.getId()).stream()
                        .map(d -> new CatalogDeliverableDto(
                                d.getId(), d.getSeqNo(), d.getDeliverableName(), d.getIsOptional()))
                        .toList();
        return new CatalogTaskDto(
                task.getId(), task.getTaskCode(), task.getTaskName(), task.getIsOptional(), deliverables);
    }
}
