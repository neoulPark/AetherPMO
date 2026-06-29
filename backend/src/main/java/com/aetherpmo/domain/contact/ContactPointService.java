package com.aetherpmo.domain.contact;

import com.aetherpmo.adapter.amaranth.UserPort;
import com.aetherpmo.adapter.amaranth.dto.UserInfo;
import com.aetherpmo.common.ApiError;
import com.aetherpmo.domain.contact.dto.ContactPointDto;
import com.aetherpmo.domain.contact.dto.ContactPointRequest;
import com.aetherpmo.domain.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactPointService {

    private static final String INTERNAL = "INTERNAL";
    private static final String EXTERNAL = "EXTERNAL";

    private final ContactPointRepository contactPointRepository;
    private final ProjectRepository projectRepository;
    private final UserPort userPort;

    @Transactional(readOnly = true)
    public List<ContactPointDto> list(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw ApiError.notFound("Project not found: " + projectId);
        }
        Map<Long, UserInfo> cache = new HashMap<>();
        return contactPointRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
                .map(c -> toDto(c, cache))
                .toList();
    }

    @Transactional
    public ContactPointDto create(Long projectId, ContactPointRequest req) {
        if (!projectRepository.existsById(projectId)) {
            throw ApiError.notFound("Project not found: " + projectId);
        }
        ContactPoint c = new ContactPoint();
        c.setProjectId(projectId);
        apply(c, req);
        return toDto(contactPointRepository.save(c), new HashMap<>());
    }

    @Transactional
    public ContactPointDto update(Long id, ContactPointRequest req) {
        ContactPoint c = load(id);
        apply(c, req);
        return toDto(c, new HashMap<>());
    }

    @Transactional
    public void delete(Long id) {
        ContactPoint c = load(id);
        contactPointRepository.delete(c);
    }

    private void apply(ContactPoint c, ContactPointRequest req) {
        String type = req.contactType();
        if (type == null || (!INTERNAL.equals(type) && !EXTERNAL.equals(type))) {
            throw ApiError.badRequest("contactType must be INTERNAL or EXTERNAL");
        }
        c.setField(req.field());
        c.setContactType(type);
        c.setUserId(req.userId());
        c.setName(req.name());
        c.setCompany(req.company());
        c.setDepartment(req.department());
        c.setTitle(req.title());
        c.setPhone(req.phone());
        c.setEmail(req.email());
        c.setNote(req.note());
        c.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);

        // For INTERNAL, optionally cache the resolved name into the row.
        if (INTERNAL.equals(type) && req.userId() != null) {
            UserInfo user = userPort.getUser(req.userId());
            if (user != null && user.fullName() != null && !user.fullName().isBlank()) {
                c.setName(user.fullName());
                if ((c.getEmail() == null || c.getEmail().isBlank())
                        && user.email() != null && !user.email().isBlank()) {
                    c.setEmail(user.email());
                }
            }
        }
    }

    private ContactPointDto toDto(ContactPoint c, Map<Long, UserInfo> cache) {
        UserInfo user = null;
        if (INTERNAL.equals(c.getContactType()) && c.getUserId() != null) {
            user = cache.computeIfAbsent(c.getUserId(), userPort::getUser);
        }
        return ContactPointDto.from(c, user);
    }

    private ContactPoint load(Long id) {
        return contactPointRepository.findById(id)
                .orElseThrow(() -> ApiError.notFound("Contact not found: " + id));
    }
}
