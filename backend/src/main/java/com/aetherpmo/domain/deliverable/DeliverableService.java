package com.aetherpmo.domain.deliverable;

import com.aetherpmo.adapter.amaranth.FilePort;
import com.aetherpmo.adapter.amaranth.dto.FileRef;
import com.aetherpmo.auth.UserEntity;
import com.aetherpmo.auth.UserRepository;
import com.aetherpmo.common.ApiError;
import com.aetherpmo.domain.deliverable.dto.AttachmentDto;
import com.aetherpmo.domain.deliverable.dto.AttachmentUploadRequest;
import com.aetherpmo.domain.deliverable.dto.DeliverableCreateRequest;
import com.aetherpmo.domain.deliverable.dto.DeliverableDto;
import com.aetherpmo.domain.deliverable.dto.DeliverableUpdateRequest;
import com.aetherpmo.domain.deliverable.dto.ReviewRequest;
import com.aetherpmo.domain.project.ProjectRepository;
import com.aetherpmo.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliverableService {

    private static final String ENTITY_TYPE = "DELIVERABLE";

    private final DeliverableRepository deliverableRepository;
    private final AttachmentRepository attachmentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final FilePort filePort;

    @Transactional(readOnly = true)
    public List<DeliverableDto> list(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw ApiError.notFound("Project not found: " + projectId);
        }
        List<Deliverable> deliverables = deliverableRepository.findByProjectIdOrderByIdAsc(projectId);
        Map<Long, String> nameCache = new HashMap<>();
        return deliverables.stream()
                .map(d -> toDto(d, nameCache))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DeliverableDto> listByTask(Long taskId) {
        List<Deliverable> deliverables = deliverableRepository.findByTaskIdOrderByIdAsc(taskId);
        Map<Long, String> nameCache = new HashMap<>();
        return deliverables.stream()
                .map(d -> toDto(d, nameCache))
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliverableDto get(Long id) {
        return toDto(load(id), new HashMap<>());
    }

    @Transactional
    public DeliverableDto create(Long projectId, DeliverableCreateRequest req) {
        if (!projectRepository.existsById(projectId)) {
            throw ApiError.notFound("Project not found: " + projectId);
        }
        Deliverable d = new Deliverable();
        d.setProjectId(projectId);
        d.setDeliverableName(req.deliverableName());
        d.setDeliverableType(req.deliverableType());
        d.setTaskId(req.taskId());
        if (req.versionNo() != null) d.setVersionNo(req.versionNo());
        if (req.status() != null) d.setStatus(req.status());
        d.setCreatedBy(currentUser.idOrNull());
        Deliverable saved = deliverableRepository.save(d);
        return toDto(saved, new HashMap<>());
    }

    @Transactional
    public DeliverableDto update(Long id, DeliverableUpdateRequest req) {
        Deliverable d = load(id);
        if (req.deliverableName() != null) d.setDeliverableName(req.deliverableName());
        if (req.deliverableType() != null) d.setDeliverableType(req.deliverableType());
        if (req.versionNo() != null) d.setVersionNo(req.versionNo());
        if (req.taskId() != null) d.setTaskId(req.taskId());
        d.setUpdatedBy(currentUser.idOrNull());
        return toDto(d, new HashMap<>());
    }

    @Transactional
    public DeliverableDto submit(Long id) {
        Deliverable d = load(id);
        requireStatus(d, "DRAFT", "submit");
        d.setStatus("SUBMITTED");
        d.setSubmittedBy(currentUser.idOrNull());
        d.setSubmittedAt(LocalDateTime.now());
        d.setUpdatedBy(currentUser.idOrNull());
        return toDto(d, new HashMap<>());
    }

    @Transactional
    public DeliverableDto startReview(Long id) {
        Deliverable d = load(id);
        requireStatus(d, "SUBMITTED", "start review");
        d.setStatus("UNDER_REVIEW");
        d.setReviewedBy(currentUser.idOrNull());
        d.setReviewedAt(LocalDateTime.now());
        d.setUpdatedBy(currentUser.idOrNull());
        return toDto(d, new HashMap<>());
    }

    @Transactional
    public DeliverableDto approve(Long id, String comment) {
        Deliverable d = load(id);
        if (!"UNDER_REVIEW".equals(d.getStatus()) && !"SUBMITTED".equals(d.getStatus())) {
            throw new IllegalStateException(
                    "Cannot approve from status " + d.getStatus() + " (expected SUBMITTED or UNDER_REVIEW)");
        }
        d.setStatus("APPROVED");
        d.setApprovedBy(currentUser.idOrNull());
        d.setApprovedAt(LocalDateTime.now());
        d.setApprovalComment(comment);
        d.setUpdatedBy(currentUser.idOrNull());
        return toDto(d, new HashMap<>());
    }

    @Transactional
    public DeliverableDto reject(Long id, String comment) {
        Deliverable d = load(id);
        if (!"UNDER_REVIEW".equals(d.getStatus()) && !"SUBMITTED".equals(d.getStatus())) {
            throw new IllegalStateException(
                    "Cannot reject from status " + d.getStatus() + " (expected SUBMITTED or UNDER_REVIEW)");
        }
        d.setStatus("REJECTED");
        d.setReviewedBy(currentUser.idOrNull());
        d.setReviewedAt(LocalDateTime.now());
        d.setReviewComment(comment);
        d.setUpdatedBy(currentUser.idOrNull());
        return toDto(d, new HashMap<>());
    }

    /**
     * Handles POST /deliverables/{id}/review. If decision is absent, transitions
     * SUBMITTED -> UNDER_REVIEW; otherwise approves or rejects.
     */
    @Transactional
    public DeliverableDto review(Long id, ReviewRequest req) {
        String decision = req == null ? null : req.decision();
        if (decision == null || decision.isBlank()) {
            return startReview(id);
        }
        String comment = req.comment();
        return switch (decision.toUpperCase()) {
            case "APPROVE" -> approve(id, comment);
            case "REJECT" -> reject(id, comment);
            default -> throw ApiError.badRequest("Unknown review decision: " + decision);
        };
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> listAttachments(Long deliverableId) {
        load(deliverableId);
        return attachmentRepository
                .findByEntityTypeAndEntityIdOrderBySortOrderAscIdAsc(ENTITY_TYPE, deliverableId)
                .stream()
                .map(AttachmentDto::from)
                .toList();
    }

    @Transactional
    public AttachmentDto addAttachment(Long deliverableId, AttachmentUploadRequest req) {
        load(deliverableId);
        FileRef ref = filePort.upload(req.fileName(), new byte[0]);

        Attachment a = new Attachment();
        a.setEntityType(ENTITY_TYPE);
        a.setEntityId(deliverableId);
        a.setFileRef(ref.fileId());
        a.setFileName(req.fileName());
        a.setContentType(req.contentType());
        a.setFileSize(req.fileSize());
        a.setSortOrder((int) attachmentRepository.countByEntityTypeAndEntityId(ENTITY_TYPE, deliverableId));
        Long uid = currentUser.idOrNull();
        a.setUploadedBy(uid == null ? null : String.valueOf(uid));
        a.setUploadedAt(LocalDateTime.now());

        return AttachmentDto.from(attachmentRepository.save(a));
    }

    private DeliverableDto toDto(Deliverable d, Map<Long, String> nameCache) {
        Long authorId = d.getSubmittedBy() != null ? d.getSubmittedBy() : d.getCreatedBy();
        String authorName = resolveName(authorId, nameCache);
        long attachmentCount = attachmentRepository.countByEntityTypeAndEntityId(ENTITY_TYPE, d.getId());
        return DeliverableDto.from(d, authorName, attachmentCount);
    }

    private String resolveName(Long userId, Map<Long, String> cache) {
        if (userId == null) {
            return null;
        }
        if (cache.containsKey(userId)) {
            return cache.get(userId);
        }
        String name = userRepository.findById(userId).map(UserEntity::getFullName).orElse(null);
        cache.put(userId, name);
        return name;
    }

    private void requireStatus(Deliverable d, String expected, String action) {
        if (!expected.equals(d.getStatus())) {
            throw new IllegalStateException(
                    "Cannot " + action + " from status " + d.getStatus() + " (expected " + expected + ")");
        }
    }

    private Deliverable load(Long id) {
        return deliverableRepository.findById(id)
                .orElseThrow(() -> ApiError.notFound("Deliverable not found: " + id));
    }
}
