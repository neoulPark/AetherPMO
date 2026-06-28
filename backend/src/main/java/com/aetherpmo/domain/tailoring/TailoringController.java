package com.aetherpmo.domain.tailoring;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.project.dto.ProjectDto;
import com.aetherpmo.domain.tailoring.dto.ProjectCreateWithTailoringRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class TailoringController {

    private final TailoringService tailoringService;

    @PostMapping("/with-tailoring")
    public ApiResponse<ProjectDto> createWithTailoring(
            @Valid @RequestBody ProjectCreateWithTailoringRequest request) {
        return ApiResponse.ok(
                tailoringService.createProjectWithTailoring(request),
                "Project created with tailoring");
    }
}
