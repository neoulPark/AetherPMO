package com.aetherpmo.domain.project.dto;

import com.aetherpmo.domain.project.Project;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectDto(
        Long id,
        String projectName,
        String projectCode,
        String description,
        Long pmId,
        Long clientCompanyId,
        String status,
        String projectStage,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        BigDecimal contractAmount,
        Integer progressRate,
        String riskLevel,
        String team,
        String location,
        String businessType,
        String bidStatus,
        String consortiumRole,
        BigDecimal consortiumShare,
        String vrbStatus,
        String announcementNo,
        LocalDate proposalDeadline,
        String pmName,
        String clientName
) {
    public static ProjectDto from(Project p) {
        return from(p, null, null);
    }

    public static ProjectDto from(Project p, String pmName, String clientName) {
        return new ProjectDto(
                p.getId(),
                p.getProjectName(),
                p.getProjectCode(),
                p.getDescription(),
                p.getPmId(),
                p.getClientCompanyId(),
                p.getStatus(),
                p.getProjectStage(),
                p.getPlannedStartDate(),
                p.getPlannedEndDate(),
                p.getActualStartDate(),
                p.getActualEndDate(),
                p.getContractAmount(),
                p.getProgressRate(),
                p.getRiskLevel(),
                p.getTeam(),
                p.getLocation(),
                p.getBusinessType(),
                p.getBidStatus(),
                p.getConsortiumRole(),
                p.getConsortiumShare(),
                p.getVrbStatus(),
                p.getAnnouncementNo(),
                p.getProposalDeadline(),
                pmName,
                clientName
        );
    }
}
