package com.aetherpmo.domain.workflow;

import com.aetherpmo.domain.workflow.dto.WorkflowDto;
import com.aetherpmo.domain.workflow.dto.WorkflowRequest;
import com.aetherpmo.domain.workflow.dto.WorkflowStatusDto;
import com.aetherpmo.domain.workflow.dto.WorkflowStatusRequest;
import com.aetherpmo.domain.workflow.dto.WorkflowTransitionDto;
import com.aetherpmo.domain.workflow.dto.WorkflowTransitionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository statusRepository;
    private final WorkflowTransitionRepository transitionRepository;

    @Transactional(readOnly = true)
    public List<WorkflowDto> listWorkflows() {
        return workflowRepository.findAll().stream()
                .map(w -> toDto(w))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowDto getWorkflow(Long id) {
        Workflow w = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        return toDto(w);
    }

    @Transactional
    public WorkflowDto createWorkflow(WorkflowRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        Workflow w = new Workflow();
        w.setName(req.name());
        w.setDescription(req.description());
        if (req.isDefault() != null) w.setIsDefault(req.isDefault());
        return toDto(workflowRepository.save(w));
    }

    @Transactional
    public WorkflowDto updateWorkflow(Long id, WorkflowRequest req) {
        Workflow w = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        if (req.name() != null) {
            if (req.name().isBlank()) throw new IllegalArgumentException("name must not be blank");
            w.setName(req.name());
        }
        if (req.description() != null) w.setDescription(req.description());
        if (req.isDefault() != null) w.setIsDefault(req.isDefault());
        return toDto(workflowRepository.save(w));
    }

    @Transactional
    public void deleteWorkflow(Long id) {
        if (!workflowRepository.existsById(id)) {
            throw new IllegalArgumentException("Workflow not found: " + id);
        }
        // DB FK ON DELETE CASCADE removes statuses + transitions;
        // catalog_node.workflow_id is set NULL by ON DELETE SET NULL.
        workflowRepository.deleteById(id);
    }

    @Transactional
    public WorkflowStatusDto addStatus(Long workflowId, WorkflowStatusRequest req) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        WorkflowStatus s = new WorkflowStatus();
        s.setWorkflowId(workflowId);
        applyStatus(s, req);
        return toStatusDto(statusRepository.save(s));
    }

    @Transactional
    public WorkflowStatusDto updateStatus(Long statusId, WorkflowStatusRequest req) {
        WorkflowStatus s = statusRepository.findById(statusId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow status not found: " + statusId));
        if (req.name() != null) {
            if (req.name().isBlank()) throw new IllegalArgumentException("name must not be blank");
            s.setName(req.name());
        }
        if (req.code() != null) s.setCode(req.code());
        if (req.color() != null) s.setColor(req.color());
        if (req.category() != null) s.setCategory(req.category());
        if (req.isInitial() != null) s.setIsInitial(req.isInitial());
        if (req.isFinal() != null) s.setIsFinal(req.isFinal());
        if (req.sortOrder() != null) s.setSortOrder(req.sortOrder());
        return toStatusDto(statusRepository.save(s));
    }

    @Transactional
    public void deleteStatus(Long statusId) {
        if (!statusRepository.existsById(statusId)) {
            throw new IllegalArgumentException("Workflow status not found: " + statusId);
        }
        statusRepository.deleteById(statusId);
    }

    @Transactional
    public WorkflowTransitionDto addTransition(Long workflowId, WorkflowTransitionRequest req) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }
        if (req.fromStatusId() == null || req.toStatusId() == null) {
            throw new IllegalArgumentException("fromStatusId and toStatusId are required");
        }
        WorkflowTransition t = new WorkflowTransition();
        t.setWorkflowId(workflowId);
        t.setFromStatusId(req.fromStatusId());
        t.setToStatusId(req.toStatusId());
        t.setName(req.name());
        return toTransitionDto(transitionRepository.save(t));
    }

    @Transactional
    public void deleteTransition(Long transitionId) {
        if (!transitionRepository.existsById(transitionId)) {
            throw new IllegalArgumentException("Workflow transition not found: " + transitionId);
        }
        transitionRepository.deleteById(transitionId);
    }

    private void applyStatus(WorkflowStatus s, WorkflowStatusRequest req) {
        s.setCode(req.code());
        s.setName(req.name());
        s.setColor(req.color());
        s.setCategory(req.category());
        if (req.isInitial() != null) s.setIsInitial(req.isInitial());
        if (req.isFinal() != null) s.setIsFinal(req.isFinal());
        if (req.sortOrder() != null) s.setSortOrder(req.sortOrder());
    }

    private WorkflowDto toDto(Workflow w) {
        List<WorkflowStatusDto> statuses =
                statusRepository.findByWorkflowIdOrderBySortOrder(w.getId()).stream()
                        .map(this::toStatusDto)
                        .toList();
        List<WorkflowTransitionDto> transitions =
                transitionRepository.findByWorkflowId(w.getId()).stream()
                        .map(this::toTransitionDto)
                        .toList();
        return new WorkflowDto(
                w.getId(), w.getName(), w.getDescription(), w.getIsDefault(), statuses, transitions);
    }

    private WorkflowStatusDto toStatusDto(WorkflowStatus s) {
        return new WorkflowStatusDto(
                s.getId(), s.getCode(), s.getName(), s.getColor(), s.getCategory(),
                s.getIsInitial(), s.getIsFinal(), s.getSortOrder());
    }

    private WorkflowTransitionDto toTransitionDto(WorkflowTransition t) {
        return new WorkflowTransitionDto(
                t.getId(), t.getFromStatusId(), t.getToStatusId(), t.getName());
    }
}
