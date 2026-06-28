package com.aetherpmo.domain.deliverable;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.deliverable.dto.AttachmentDto;
import com.aetherpmo.domain.deliverable.dto.AttachmentUploadRequest;
import com.aetherpmo.domain.deliverable.dto.DeliverableCreateRequest;
import com.aetherpmo.domain.deliverable.dto.DeliverableDto;
import com.aetherpmo.domain.deliverable.dto.DeliverableUpdateRequest;
import com.aetherpmo.domain.deliverable.dto.ReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class DeliverableController {

    private final DeliverableService deliverableService;

    @GetMapping("/projects/{projectId}/deliverables")
    public ApiResponse<List<DeliverableDto>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(deliverableService.list(projectId));
    }

    @PostMapping("/projects/{projectId}/deliverables")
    public ApiResponse<DeliverableDto> create(@PathVariable Long projectId,
                                              @Valid @RequestBody DeliverableCreateRequest request) {
        return ApiResponse.ok(deliverableService.create(projectId, request), "Deliverable created");
    }

    @GetMapping("/deliverables/{id}")
    public ApiResponse<DeliverableDto> get(@PathVariable Long id) {
        return ApiResponse.ok(deliverableService.get(id));
    }

    @PutMapping("/deliverables/{id}")
    public ApiResponse<DeliverableDto> update(@PathVariable Long id,
                                              @RequestBody DeliverableUpdateRequest request) {
        return ApiResponse.ok(deliverableService.update(id, request), "Deliverable updated");
    }

    @PostMapping("/deliverables/{id}/submit")
    public ApiResponse<DeliverableDto> submit(@PathVariable Long id) {
        return ApiResponse.ok(deliverableService.submit(id), "Deliverable submitted");
    }

    @PostMapping("/deliverables/{id}/review")
    public ApiResponse<DeliverableDto> review(@PathVariable Long id,
                                              @RequestBody(required = false) ReviewRequest request) {
        return ApiResponse.ok(deliverableService.review(id, request), "Review processed");
    }

    @PostMapping("/deliverables/{id}/approve")
    public ApiResponse<DeliverableDto> approve(@PathVariable Long id,
                                               @RequestBody(required = false) ReviewRequest request) {
        String comment = request == null ? null : request.comment();
        return ApiResponse.ok(deliverableService.approve(id, comment), "Deliverable approved");
    }

    @GetMapping("/deliverables/{id}/attachments")
    public ApiResponse<List<AttachmentDto>> listAttachments(@PathVariable Long id) {
        return ApiResponse.ok(deliverableService.listAttachments(id));
    }

    @PostMapping("/deliverables/{id}/attachments")
    public ApiResponse<AttachmentDto> addAttachment(@PathVariable Long id,
                                                    @Valid @RequestBody AttachmentUploadRequest request) {
        return ApiResponse.ok(deliverableService.addAttachment(id, request), "Attachment uploaded");
    }
}
