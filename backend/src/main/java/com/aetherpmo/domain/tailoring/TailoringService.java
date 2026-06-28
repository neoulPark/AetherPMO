package com.aetherpmo.domain.tailoring;

import com.aetherpmo.domain.deliverable.Deliverable;
import com.aetherpmo.domain.deliverable.DeliverableRepository;
import com.aetherpmo.domain.methodology.DeliverableTemplate;
import com.aetherpmo.domain.methodology.DeliverableTemplateRepository;
import com.aetherpmo.domain.methodology.MethodologyActivity;
import com.aetherpmo.domain.methodology.MethodologyActivityRepository;
import com.aetherpmo.domain.methodology.MethodologyPhase;
import com.aetherpmo.domain.methodology.MethodologyPhaseRepository;
import com.aetherpmo.domain.methodology.TaskTemplate;
import com.aetherpmo.domain.methodology.TaskTemplateRepository;
import com.aetherpmo.domain.project.Project;
import com.aetherpmo.domain.project.ProjectRepository;
import com.aetherpmo.domain.project.dto.ProjectDto;
import com.aetherpmo.domain.task.Task;
import com.aetherpmo.domain.task.TaskRepository;
import com.aetherpmo.domain.tailoring.dto.ProjectCreateWithTailoringRequest;
import com.aetherpmo.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TailoringService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DeliverableRepository deliverableRepository;
    private final ProjectTailoringRepository tailoringRepository;
    private final MethodologyPhaseRepository phaseRepository;
    private final MethodologyActivityRepository activityRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final DeliverableTemplateRepository deliverableTemplateRepository;
    private final CurrentUser currentUser;

    @Transactional
    public ProjectDto createProjectWithTailoring(ProjectCreateWithTailoringRequest req) {
        Long userId = currentUser.idOrNull();

        // 1. Create the project.
        Project p = new Project();
        p.setProjectName(req.projectName());
        p.setProjectCode(req.projectCode());
        p.setDescription(req.description());
        p.setPmId(req.pmId());
        p.setClientCompanyId(req.clientCompanyId());
        if (req.status() != null) p.setStatus(req.status());
        if (req.projectStage() != null) p.setProjectStage(req.projectStage());
        p.setPlannedStartDate(req.plannedStartDate());
        p.setPlannedEndDate(req.plannedEndDate());
        p.setContractAmount(req.contractAmount());
        if (req.riskLevel() != null) p.setRiskLevel(req.riskLevel());
        p.setTeam(req.team());
        p.setLocation(req.location());
        p.setBusinessType(req.businessType());
        p.setCreatedBy(userId);
        p.setUpdatedBy(userId);
        Project project = projectRepository.save(p);

        List<Long> taskTplIds = req.selectedTaskTemplateIds() == null
                ? List.of() : req.selectedTaskTemplateIds();
        List<Long> delivTplIds = req.selectedDeliverableTemplateIds() == null
                ? List.of() : req.selectedDeliverableTemplateIds();

        // 2. Load selected task templates with activity + phase context.
        List<TaskTemplate> taskTemplates = taskTplIds.isEmpty()
                ? List.of() : taskTemplateRepository.findAllById(taskTplIds);

        Map<Long, MethodologyActivity> activityById = new HashMap<>();
        Map<Long, MethodologyPhase> phaseById = new HashMap<>();
        for (MethodologyActivity a : activityRepository.findAll()) activityById.put(a.getId(), a);
        for (MethodologyPhase ph : phaseRepository.findAll()) phaseById.put(ph.getId(), ph);

        // Group selected task templates by phase, ordered by phase.sort_order.
        Map<MethodologyPhase, List<TaskTemplate>> byPhase = new LinkedHashMap<>();
        List<MethodologyPhase> orderedPhases = phaseRepository.findAllByOrderBySortOrderAscIdAsc();
        for (MethodologyPhase ph : orderedPhases) byPhase.put(ph, new ArrayList<>());
        for (TaskTemplate tt : taskTemplates) {
            MethodologyActivity act = activityById.get(tt.getActivityId());
            if (act == null) continue;
            MethodologyPhase ph = phaseById.get(act.getPhaseId());
            if (ph == null) continue;
            byPhase.computeIfAbsent(ph, k -> new ArrayList<>()).add(tt);
        }

        // taskTemplateId -> generated child Task id (for deliverable linkage + tailoring).
        Map<Long, Long> generatedTaskIdByTemplate = new HashMap<>();
        int phaseOrder = 0;

        // 3-4. Create top-level phase tasks + child task-template tasks.
        for (Map.Entry<MethodologyPhase, List<TaskTemplate>> e : byPhase.entrySet()) {
            List<TaskTemplate> tts = e.getValue();
            if (tts.isEmpty()) continue;
            MethodologyPhase ph = e.getKey();

            Task phaseTask = new Task();
            phaseTask.setProjectId(project.getId());
            phaseTask.setParentTaskId(null);
            phaseTask.setDepth(0);
            phaseTask.setSortOrder(phaseOrder++);
            phaseTask.setTaskName(ph.getPhaseName());
            phaseTask.setStatus("TODO");
            phaseTask.setProgressRate(0);
            phaseTask.setCreatedBy(userId);
            phaseTask.setUpdatedBy(userId);
            Task savedPhaseTask = taskRepository.save(phaseTask);

            // Order children by activity.sort_order then task_template.sort_order.
            tts.sort(Comparator
                    .comparingInt((TaskTemplate t) -> {
                        MethodologyActivity a = activityById.get(t.getActivityId());
                        return a == null ? 0 : a.getSortOrder();
                    })
                    .thenComparingInt(TaskTemplate::getSortOrder)
                    .thenComparing(TaskTemplate::getId));

            int childOrder = 0;
            for (TaskTemplate tt : tts) {
                Task childTask = new Task();
                childTask.setProjectId(project.getId());
                childTask.setParentTaskId(savedPhaseTask.getId());
                childTask.setDepth(1);
                childTask.setSortOrder(childOrder++);
                childTask.setTaskName(tt.getTaskName());
                childTask.setTaskTemplateId(tt.getId());
                childTask.setStatus("TODO");
                childTask.setProgressRate(0);
                childTask.setCreatedBy(userId);
                childTask.setUpdatedBy(userId);
                Task savedChild = taskRepository.save(childTask);
                generatedTaskIdByTemplate.put(tt.getId(), savedChild.getId());

                // Tailoring record for the task template selection.
                ProjectTailoring tr = new ProjectTailoring();
                tr.setProjectId(project.getId());
                tr.setTaskTemplateId(tt.getId());
                tr.setIsSelected(true);
                tr.setGeneratedTaskId(savedChild.getId());
                tailoringRepository.save(tr);
            }
        }

        // 5-6. Create deliverables from selected deliverable templates.
        List<DeliverableTemplate> delivTemplates = delivTplIds.isEmpty()
                ? List.of() : deliverableTemplateRepository.findAllById(delivTplIds);
        for (DeliverableTemplate dt : delivTemplates) {
            Deliverable d = new Deliverable();
            d.setProjectId(project.getId());
            d.setDeliverableName(dt.getDeliverableName());
            d.setDeliverableType(dt.getDeliverableCategory());
            d.setStatus("DRAFT");
            d.setVersionNo(dt.getVersionNo() != null ? dt.getVersionNo() : "1.0");
            d.setDeliverableTemplateId(dt.getId());
            Long generatedTaskId = generatedTaskIdByTemplate.get(dt.getTaskTemplateId());
            if (generatedTaskId != null) d.setTaskId(generatedTaskId);
            d.setCreatedBy(userId);
            d.setUpdatedBy(userId);
            Deliverable savedDeliverable = deliverableRepository.save(d);

            ProjectTailoring tr = new ProjectTailoring();
            tr.setProjectId(project.getId());
            tr.setDeliverableTemplateId(dt.getId());
            tr.setIsSelected(true);
            tr.setGeneratedDeliverableId(savedDeliverable.getId());
            tailoringRepository.save(tr);
        }

        return ProjectDto.from(project);
    }
}
