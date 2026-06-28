package com.aetherpmo.domain.project;

import com.aetherpmo.common.ApiError;
import com.aetherpmo.domain.project.dto.ProjectCreateRequest;
import com.aetherpmo.domain.project.dto.ProjectDto;
import com.aetherpmo.domain.project.dto.ProjectSummaryDto;
import com.aetherpmo.domain.project.dto.ProjectUpdateRequest;
import com.aetherpmo.auth.UserEntity;
import com.aetherpmo.auth.UserRepository;
import com.aetherpmo.domain.company.CompanyNameRepository;
import com.aetherpmo.domain.task.TaskRepository;
import com.aetherpmo.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CompanyNameRepository companyNameRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<ProjectDto> list(String stage) {
        List<Project> projects = (stage == null || stage.isBlank())
                ? projectRepository.findAllByOrderByIdAsc()
                : projectRepository.findByProjectStageOrderByIdAsc(stage);
        return projects.stream().map(this::toDtoEnriched).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long id) {
        return toDtoEnriched(load(id));
    }

    private ProjectDto toDtoEnriched(Project p) {
        String pmName = p.getPmId() == null ? null
                : userRepository.findById(p.getPmId()).map(UserEntity::getFullName).orElse(null);
        String clientName = companyNameRepository.findNameById(p.getClientCompanyId());
        return ProjectDto.from(p, pmName, clientName);
    }

    @Transactional
    public ProjectDto create(ProjectCreateRequest req) {
        Project p = new Project();
        p.setProjectName(req.projectName());
        p.setProjectCode(req.projectCode());
        p.setDescription(req.description());
        p.setPmId(req.pmId());
        p.setClientCompanyId(req.clientCompanyId());
        if (req.status() != null) p.setStatus(req.status());
        if (req.projectStage() != null) p.setProjectStage(req.projectStage());
        p.setPlannedStartDate(req.plannedStartDate());
        p.setPlannedEndDate(req.plannedEndDate());
        p.setContractAmount(req.contractAmount());
        p.setTeam(req.team());
        p.setLocation(req.location());
        p.setBusinessType(req.businessType());
        p.setCreatedBy(currentUser.idOrNull());
        p.setUpdatedBy(currentUser.idOrNull());
        return ProjectDto.from(projectRepository.save(p));
    }

    @Transactional
    public ProjectDto update(Long id, ProjectUpdateRequest req) {
        Project p = load(id);
        if (req.projectName() != null) p.setProjectName(req.projectName());
        if (req.projectCode() != null) p.setProjectCode(req.projectCode());
        if (req.description() != null) p.setDescription(req.description());
        if (req.pmId() != null) p.setPmId(req.pmId());
        if (req.clientCompanyId() != null) p.setClientCompanyId(req.clientCompanyId());
        if (req.status() != null) p.setStatus(req.status());
        if (req.projectStage() != null) p.setProjectStage(req.projectStage());
        if (req.plannedStartDate() != null) p.setPlannedStartDate(req.plannedStartDate());
        if (req.plannedEndDate() != null) p.setPlannedEndDate(req.plannedEndDate());
        if (req.actualStartDate() != null) p.setActualStartDate(req.actualStartDate());
        if (req.actualEndDate() != null) p.setActualEndDate(req.actualEndDate());
        if (req.contractAmount() != null) p.setContractAmount(req.contractAmount());
        if (req.progressRate() != null) p.setProgressRate(req.progressRate());
        if (req.riskLevel() != null) p.setRiskLevel(req.riskLevel());
        if (req.team() != null) p.setTeam(req.team());
        if (req.location() != null) p.setLocation(req.location());
        if (req.businessType() != null) p.setBusinessType(req.businessType());
        p.setUpdatedBy(currentUser.idOrNull());
        return ProjectDto.from(p);
    }

    @Transactional(readOnly = true)
    public ProjectSummaryDto summary(Long id) {
        Project p = load(id);
        long total = taskRepository.countByProjectId(id);
        long todo = taskRepository.countByProjectIdAndStatus(id, "TODO");
        long inProgress = taskRepository.countByProjectIdAndStatus(id, "IN_PROGRESS");
        long review = taskRepository.countByProjectIdAndStatus(id, "REVIEW");
        long done = taskRepository.countByProjectIdAndStatus(id, "DONE");
        return new ProjectSummaryDto(
                p.getId(), p.getProjectName(), p.getProgressRate(),
                total, todo, inProgress, review, done
        );
    }

    private Project load(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ApiError.notFound("Project not found: " + id));
    }
}
