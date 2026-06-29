package com.aetherpmo.domain.contact;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.contact.dto.ContactPointDto;
import com.aetherpmo.domain.contact.dto.ContactPointRequest;
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
public class ContactPointController {

    private final ContactPointService contactPointService;

    @GetMapping("/projects/{projectId}/contacts")
    public ApiResponse<List<ContactPointDto>> list(@PathVariable Long projectId) {
        return ApiResponse.ok(contactPointService.list(projectId));
    }

    @PostMapping("/projects/{projectId}/contacts")
    public ApiResponse<ContactPointDto> create(@PathVariable Long projectId,
                                               @RequestBody ContactPointRequest request) {
        return ApiResponse.ok(contactPointService.create(projectId, request), "Contact created");
    }

    @PutMapping("/contacts/{id}")
    public ApiResponse<ContactPointDto> update(@PathVariable Long id,
                                               @RequestBody ContactPointRequest request) {
        return ApiResponse.ok(contactPointService.update(id, request), "Contact updated");
    }

    @DeleteMapping("/contacts/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contactPointService.delete(id);
        return ApiResponse.ok(null, "Contact deleted");
    }
}
