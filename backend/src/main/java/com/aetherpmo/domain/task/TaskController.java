package com.aetherpmo.domain.task;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.deliverable.dto.DeliverableDto;
import com.aetherpmo.domain.task.dto.AssignRequest;
import com.aetherpmo.domain.task.dto.ProgressUpdateRequest;
import com.aetherpmo.domain.task.dto.TaskCreateRequest;
import com.aetherpmo.domain.task.dto.TaskDeliverableCreateRequest;
import com.aetherpmo.domain.task.dto.TaskDto;
import com.aetherpmo.domain.task.dto.TaskTreeDto;
import com.aetherpmo.domain.task.dto.TaskUpdateRequest;
import com.aetherpmo.domain.task.dto.TemplateDeliverableDto;
import com.aetherpmo.domain.workflow.dto.WorkflowDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    public ApiResponse<List<TaskTreeDto>> tree(@PathVariable Long projectId) {
        return ApiResponse.ok(taskService.tree(projectId));
    }

    @PostMapping("/api/v1/projects/{projectId}/tasks")
    public ApiResponse<TaskDto> create(@PathVariable Long projectId,
                                       @Valid @RequestBody TaskCreateRequest request) {
        return ApiResponse.ok(taskService.create(projectId, request), "Task created");
    }

    @GetMapping("/api/v1/tasks/{id}")
    public ApiResponse<TaskDto> get(@PathVariable Long id) {
        return ApiResponse.ok(taskService.get(id));
    }

    @PutMapping("/api/v1/tasks/{id}")
    public ApiResponse<TaskDto> update(@PathVariable Long id,
                                       @RequestBody TaskUpdateRequest request) {
        return ApiResponse.ok(taskService.update(id, request), "Task updated");
    }

    @DeleteMapping("/api/v1/tasks/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.ok(null, "Task deleted");
    }

    @PutMapping("/api/v1/tasks/{id}/progress")
    public ApiResponse<TaskDto> updateProgress(@PathVariable Long id,
                                               @Valid @RequestBody ProgressUpdateRequest request) {
        return ApiResponse.ok(taskService.updateProgress(id, request), "Progress updated");
    }

    @PostMapping("/api/v1/tasks/{id}/assign")
    public ApiResponse<TaskDto> assign(@PathVariable Long id,
                                       @Valid @RequestBody AssignRequest request) {
        return ApiResponse.ok(taskService.assign(id, request), "Assignee updated");
    }

    @GetMapping("/api/v1/tasks/{id}/assignment-history")
    public ApiResponse<List<TaskAssignmentHistory>> history(@PathVariable Long id) {
        return ApiResponse.ok(taskService.history(id));
    }

    @GetMapping("/api/v1/tasks/{id}/workflow")
    public ApiResponse<WorkflowDto> workflow(@PathVariable Long id) {
        return ApiResponse.ok(taskService.workflow(id));
    }

    @GetMapping("/api/v1/tasks/{id}/deliverables")
    public ApiResponse<List<DeliverableDto>> deliverables(@PathVariable Long id) {
        return ApiResponse.ok(taskService.deliverables(id));
    }

    @PostMapping("/api/v1/tasks/{id}/deliverables")
    public ApiResponse<DeliverableDto> createDeliverable(@PathVariable Long id,
                                                         @Valid @RequestBody TaskDeliverableCreateRequest request) {
        return ApiResponse.ok(taskService.createDeliverable(id, request), "Deliverable created");
    }

    @GetMapping("/api/v1/tasks/{id}/template-deliverables")
    public ApiResponse<List<TemplateDeliverableDto>> templateDeliverables(@PathVariable Long id) {
        return ApiResponse.ok(taskService.templateDeliverables(id));
    }
}
