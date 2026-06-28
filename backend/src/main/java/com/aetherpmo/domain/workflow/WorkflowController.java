package com.aetherpmo.domain.workflow;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.workflow.dto.WorkflowDto;
import com.aetherpmo.domain.workflow.dto.WorkflowRequest;
import com.aetherpmo.domain.workflow.dto.WorkflowStatusDto;
import com.aetherpmo.domain.workflow.dto.WorkflowStatusRequest;
import com.aetherpmo.domain.workflow.dto.WorkflowTransitionDto;
import com.aetherpmo.domain.workflow.dto.WorkflowTransitionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping("/workflows")
    public ApiResponse<List<WorkflowDto>> listWorkflows() {
        return ApiResponse.ok(workflowService.listWorkflows());
    }

    @GetMapping("/workflows/{id}")
    public ApiResponse<WorkflowDto> getWorkflow(@PathVariable Long id) {
        return ApiResponse.ok(workflowService.getWorkflow(id));
    }

    @PostMapping("/workflows")
    public ApiResponse<WorkflowDto> createWorkflow(@RequestBody WorkflowRequest request) {
        return ApiResponse.ok(workflowService.createWorkflow(request), "Workflow created");
    }

    @PutMapping("/workflows/{id}")
    public ApiResponse<WorkflowDto> updateWorkflow(
            @PathVariable Long id, @RequestBody WorkflowRequest request) {
        return ApiResponse.ok(workflowService.updateWorkflow(id, request), "Workflow updated");
    }

    @DeleteMapping("/workflows/{id}")
    public ApiResponse<Void> deleteWorkflow(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ApiResponse.ok(null, "Workflow deleted");
    }

    @PostMapping("/workflows/{id}/statuses")
    public ApiResponse<WorkflowStatusDto> addStatus(
            @PathVariable Long id, @RequestBody WorkflowStatusRequest request) {
        return ApiResponse.ok(workflowService.addStatus(id, request), "Status added");
    }

    @PutMapping("/workflow-statuses/{statusId}")
    public ApiResponse<WorkflowStatusDto> updateStatus(
            @PathVariable Long statusId, @RequestBody WorkflowStatusRequest request) {
        return ApiResponse.ok(workflowService.updateStatus(statusId, request), "Status updated");
    }

    @DeleteMapping("/workflow-statuses/{statusId}")
    public ApiResponse<Void> deleteStatus(@PathVariable Long statusId) {
        workflowService.deleteStatus(statusId);
        return ApiResponse.ok(null, "Status deleted");
    }

    @PostMapping("/workflows/{id}/transitions")
    public ApiResponse<WorkflowTransitionDto> addTransition(
            @PathVariable Long id, @RequestBody WorkflowTransitionRequest request) {
        return ApiResponse.ok(workflowService.addTransition(id, request), "Transition added");
    }

    @DeleteMapping("/workflow-transitions/{transitionId}")
    public ApiResponse<Void> deleteTransition(@PathVariable Long transitionId) {
        workflowService.deleteTransition(transitionId);
        return ApiResponse.ok(null, "Transition deleted");
    }
}
