package com.aetherpmo.domain.task;

import com.aetherpmo.common.ApiError;
import com.aetherpmo.domain.project.ProjectRepository;
import com.aetherpmo.domain.task.dto.AssignRequest;
import com.aetherpmo.domain.task.dto.ProgressUpdateRequest;
import com.aetherpmo.domain.task.dto.TaskCreateRequest;
import com.aetherpmo.domain.task.dto.TaskDto;
import com.aetherpmo.domain.task.dto.TaskTreeDto;
import com.aetherpmo.domain.task.dto.TaskUpdateRequest;
import com.aetherpmo.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssignmentHistoryRepository historyRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<TaskTreeDto> tree(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw ApiError.notFound("Project not found: " + projectId);
        }
        List<Task> tasks = taskRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId);

        Map<Long, TaskTreeDto> nodes = new LinkedHashMap<>();
        for (Task t : tasks) {
            nodes.put(t.getId(), TaskTreeDto.from(t));
        }

        List<TaskTreeDto> roots = new ArrayList<>();
        for (Task t : tasks) {
            TaskTreeDto node = nodes.get(t.getId());
            if (t.getParentTaskId() != null && nodes.containsKey(t.getParentTaskId())) {
                nodes.get(t.getParentTaskId()).children().add(node);
            } else {
                roots.add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long id) {
        return TaskDto.from(load(id));
    }

    @Transactional
    public TaskDto create(Long projectId, TaskCreateRequest req) {
        if (!projectRepository.existsById(projectId)) {
            throw ApiError.notFound("Project not found: " + projectId);
        }

        Task t = new Task();
        t.setProjectId(projectId);
        t.setTaskName(req.taskName());
        t.setAssigneeId(req.assigneeId());
        t.setPlannedStartDate(req.plannedStartDate());
        t.setPlannedEndDate(req.plannedEndDate());
        t.setPlannedEffort(req.plannedEffort());
        t.setDescription(req.description());
        if (req.status() != null) t.setStatus(req.status());
        if (req.sortOrder() != null) t.setSortOrder(req.sortOrder());
        if (req.progressRate() != null) t.setProgressRate(req.progressRate());

        if (req.parentTaskId() != null) {
            Task parent = load(req.parentTaskId());
            if (!parent.getProjectId().equals(projectId)) {
                throw ApiError.badRequest("Parent task belongs to a different project");
            }
            t.setParentTaskId(parent.getId());
            t.setDepth(parent.getDepth() == null ? 1 : parent.getDepth() + 1);
        } else {
            t.setDepth(0);
        }

        Task saved = taskRepository.save(t);

        // A new child means the parent is no longer a leaf -> recalc.
        if (saved.getParentTaskId() != null) {
            recalcAncestors(saved.getParentTaskId());
        }
        return TaskDto.from(saved);
    }

    @Transactional
    public TaskDto update(Long id, TaskUpdateRequest req) {
        Task t = load(id);
        if (req.taskName() != null) t.setTaskName(req.taskName());
        if (req.status() != null) t.setStatus(req.status());
        if (req.assigneeId() != null) t.setAssigneeId(req.assigneeId());
        if (req.plannedStartDate() != null) t.setPlannedStartDate(req.plannedStartDate());
        if (req.plannedEndDate() != null) t.setPlannedEndDate(req.plannedEndDate());
        if (req.actualStartDate() != null) t.setActualStartDate(req.actualStartDate());
        if (req.actualEndDate() != null) t.setActualEndDate(req.actualEndDate());
        if (req.plannedEffort() != null) t.setPlannedEffort(req.plannedEffort());
        if (req.actualEffort() != null) t.setActualEffort(req.actualEffort());
        if (req.sortOrder() != null) t.setSortOrder(req.sortOrder());
        if (req.description() != null) t.setDescription(req.description());
        t.setUpdatedBy(currentUser.idOrNull());
        return TaskDto.from(t);
    }

    @Transactional
    public void delete(Long id) {
        Task t = load(id);
        Long parentId = t.getParentTaskId();
        taskRepository.delete(t); // children cascade via FK ON DELETE CASCADE
        taskRepository.flush();
        if (parentId != null) {
            recalcAncestors(parentId);
        }
    }

    @Transactional
    public TaskDto updateProgress(Long id, ProgressUpdateRequest req) {
        Task t = load(id);
        List<Task> children = taskRepository.findByParentTaskIdOrderBySortOrderAscIdAsc(id);
        if (!children.isEmpty()) {
            throw ApiError.badRequest("Cannot set progress directly on a parent task; it is computed from children");
        }

        t.setProgressRate(req.progressRate());
        if (req.progressRate() == 100) {
            t.setStatus("DONE");
        } else if (req.progressRate() > 0 && "TODO".equals(t.getStatus())) {
            t.setStatus("IN_PROGRESS");
        }
        t.setUpdatedBy(currentUser.idOrNull());

        if (t.getParentTaskId() != null) {
            recalcAncestors(t.getParentTaskId());
        }
        return TaskDto.from(t);
    }

    @Transactional
    public TaskDto assign(Long id, AssignRequest req) {
        Task t = load(id);
        Long from = t.getAssigneeId();
        t.setAssigneeId(req.assigneeId());
        t.setUpdatedBy(currentUser.idOrNull());

        TaskAssignmentHistory h = new TaskAssignmentHistory();
        h.setTaskId(id);
        h.setFromUserId(from);
        h.setToUserId(req.assigneeId());
        h.setChangedBy(currentUser.idOrNull());
        h.setChangeReason(req.reason());
        historyRepository.save(h);

        return TaskDto.from(t);
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentHistory> history(Long id) {
        load(id);
        return historyRepository.findByTaskIdOrderByChangedAtDesc(id);
    }

    /**
     * Recalculate progress for the task (as a parent) and propagate up the tree.
     */
    private void recalcAncestors(Long taskId) {
        Long current = taskId;
        while (current != null) {
            Task task = taskRepository.findById(current).orElse(null);
            if (task == null) {
                break;
            }
            List<Task> children = taskRepository.findByParentTaskIdOrderBySortOrderAscIdAsc(current);
            if (!children.isEmpty()) {
                task.setProgressRate(weightedProgress(children));
                applyDerivedStatus(task);
            }
            current = task.getParentTaskId();
        }
    }

    private int weightedProgress(List<Task> children) {
        boolean allHaveEffort = children.stream()
                .allMatch(c -> c.getPlannedEffort() != null && c.getPlannedEffort().signum() > 0);

        if (allHaveEffort) {
            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal weightedSum = BigDecimal.ZERO;
            for (Task c : children) {
                BigDecimal w = c.getPlannedEffort();
                totalWeight = totalWeight.add(w);
                weightedSum = weightedSum.add(w.multiply(BigDecimal.valueOf(progressOf(c))));
            }
            if (totalWeight.signum() == 0) {
                return equalAverage(children);
            }
            return weightedSum.divide(totalWeight, 0, RoundingMode.HALF_UP).intValue();
        }
        return equalAverage(children);
    }

    private int equalAverage(List<Task> children) {
        int sum = children.stream().mapToInt(this::progressOf).sum();
        return Math.round((float) sum / children.size());
    }

    private int progressOf(Task t) {
        return t.getProgressRate() == null ? 0 : t.getProgressRate();
    }

    private void applyDerivedStatus(Task task) {
        int p = progressOf(task);
        if (p >= 100) {
            task.setStatus("DONE");
        } else if (p > 0) {
            task.setStatus("IN_PROGRESS");
        } else {
            task.setStatus("TODO");
        }
    }

    private Task load(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> ApiError.notFound("Task not found: " + id));
    }
}
