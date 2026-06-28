package com.aetherpmo.domain.tailoring;

import com.aetherpmo.domain.deliverable.Deliverable;
import com.aetherpmo.domain.deliverable.DeliverableRepository;
import com.aetherpmo.domain.methodology.CatalogNode;
import com.aetherpmo.domain.methodology.CatalogNodeRepository;
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
    private final CatalogNodeRepository nodeRepository;
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

        List<Long> selectedNodeIds = req.selectedNodeIds() == null
                ? List.of() : req.selectedNodeIds();
        if (selectedNodeIds.isEmpty()) {
            return ProjectDto.from(project);
        }

        // 2. Load selected nodes and partition by type.
        List<CatalogNode> selectedNodes = nodeRepository.findAllById(selectedNodeIds);

        // Index every node by id for ancestor walking.
        Map<Long, CatalogNode> nodeById = new HashMap<>();
        for (CatalogNode n : nodeRepository.findAll()) nodeById.put(n.getId(), n);

        List<CatalogNode> taskNodes = new ArrayList<>();
        List<CatalogNode> deliverableNodes = new ArrayList<>();
        for (CatalogNode n : selectedNodes) {
            if ("TASK".equals(n.getNodeType())) taskNodes.add(n);
            else if ("DELIVERABLE".equals(n.getNodeType())) deliverableNodes.add(n);
        }

        // 3. Group TASK nodes by PHASE ancestor.
        Map<CatalogNode, List<CatalogNode>> byPhase = new LinkedHashMap<>();
        for (CatalogNode taskNode : taskNodes) {
            CatalogNode activity = parentOf(taskNode, nodeById);
            CatalogNode phase = parentOf(activity, nodeById);
            if (phase == null) continue;
            byPhase.computeIfAbsent(phase, k -> new ArrayList<>()).add(taskNode);
        }

        // Order phases by their sort order.
        List<CatalogNode> orderedPhases = new ArrayList<>(byPhase.keySet());
        orderedPhases.sort(Comparator
                .comparingInt(CatalogNode::getSortOrder)
                .thenComparing(CatalogNode::getId));

        // catalogNodeId(TASK) -> generated child Task id (for deliverable linkage).
        Map<Long, Long> generatedTaskIdByNode = new HashMap<>();
        int phaseOrder = 0;

        for (CatalogNode phase : orderedPhases) {
            List<CatalogNode> tns = byPhase.get(phase);
            if (tns == null || tns.isEmpty()) continue;

            Task phaseTask = new Task();
            phaseTask.setProjectId(project.getId());
            phaseTask.setParentTaskId(null);
            phaseTask.setDepth(0);
            phaseTask.setSortOrder(phaseOrder++);
            phaseTask.setTaskName(phase.getName());
            phaseTask.setStatus("TODO");
            phaseTask.setProgressRate(0);
            phaseTask.setCreatedBy(userId);
            phaseTask.setUpdatedBy(userId);
            Task savedPhaseTask = taskRepository.save(phaseTask);

            // Order children by ancestor activity sort then task sort.
            tns.sort(Comparator
                    .comparingInt((CatalogNode t) -> {
                        CatalogNode a = parentOf(t, nodeById);
                        return a == null ? 0 : a.getSortOrder();
                    })
                    .thenComparingInt(CatalogNode::getSortOrder)
                    .thenComparing(CatalogNode::getId));

            int childOrder = 0;
            for (CatalogNode taskNode : tns) {
                Task childTask = new Task();
                childTask.setProjectId(project.getId());
                childTask.setParentTaskId(savedPhaseTask.getId());
                childTask.setDepth(1);
                childTask.setSortOrder(childOrder++);
                childTask.setTaskName(taskNode.getName());
                childTask.setCatalogNodeId(taskNode.getId());
                childTask.setStatus("TODO");
                childTask.setProgressRate(0);
                childTask.setCreatedBy(userId);
                childTask.setUpdatedBy(userId);
                Task savedChild = taskRepository.save(childTask);
                generatedTaskIdByNode.put(taskNode.getId(), savedChild.getId());

                ProjectTailoring tr = new ProjectTailoring();
                tr.setProjectId(project.getId());
                tr.setCatalogNodeId(taskNode.getId());
                tr.setIsSelected(true);
                tr.setGeneratedTaskId(savedChild.getId());
                tailoringRepository.save(tr);
            }
        }

        // 4. Create deliverables from selected DELIVERABLE nodes.
        for (CatalogNode dNode : deliverableNodes) {
            Deliverable d = new Deliverable();
            d.setProjectId(project.getId());
            d.setDeliverableName(dNode.getName());
            d.setDeliverableType(dNode.getDeliverableCategory());
            d.setStatus("DRAFT");
            d.setVersionNo("1.0");
            d.setCatalogNodeId(dNode.getId());
            // Link to the generated task if the parent TASK node was selected.
            Long parentTaskNodeId = dNode.getParentNodeId();
            if (parentTaskNodeId != null) {
                Long generatedTaskId = generatedTaskIdByNode.get(parentTaskNodeId);
                if (generatedTaskId != null) d.setTaskId(generatedTaskId);
            }
            d.setCreatedBy(userId);
            d.setUpdatedBy(userId);
            Deliverable savedDeliverable = deliverableRepository.save(d);

            ProjectTailoring tr = new ProjectTailoring();
            tr.setProjectId(project.getId());
            tr.setCatalogNodeId(dNode.getId());
            tr.setIsSelected(true);
            tr.setGeneratedDeliverableId(savedDeliverable.getId());
            tailoringRepository.save(tr);
        }

        return ProjectDto.from(project);
    }

    private CatalogNode parentOf(CatalogNode node, Map<Long, CatalogNode> nodeById) {
        if (node == null || node.getParentNodeId() == null) return null;
        return nodeById.get(node.getParentNodeId());
    }
}
