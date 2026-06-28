package com.aetherpmo.domain.project;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.project.dto.ProjectCreateRequest;
import com.aetherpmo.domain.project.dto.ProjectDto;
import com.aetherpmo.domain.project.dto.ProjectSummaryDto;
import com.aetherpmo.domain.project.dto.ProjectUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ApiResponse<List<ProjectDto>> list(@RequestParam(required = false) String stage) {
        return ApiResponse.ok(projectService.list(stage));
    }

    @PostMapping
    public ApiResponse<ProjectDto> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiResponse.ok(projectService.create(request), "Project created");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDto> get(@PathVariable Long id) {
        return ApiResponse.ok(projectService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectDto> update(@PathVariable Long id,
                                          @RequestBody ProjectUpdateRequest request) {
        return ApiResponse.ok(projectService.update(id, request), "Project updated");
    }

    @GetMapping("/{id}/summary")
    public ApiResponse<ProjectSummaryDto> summary(@PathVariable Long id) {
        return ApiResponse.ok(projectService.summary(id));
    }
}
