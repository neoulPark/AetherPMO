package com.aetherpmo.domain.project;

import com.aetherpmo.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pms_project")
@Getter
@Setter
@NoArgsConstructor
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "project_code", unique = true, length = 50)
    private String projectCode;

    @Column(name = "description")
    private String description;

    @Column(name = "pm_id")
    private Long pmId;

    @Column(name = "client_company_id")
    private Long clientCompanyId;

    @Column(name = "status", length = 20)
    private String status = "PLANNING";

    @Column(name = "project_stage", length = 20)
    private String projectStage = "EXECUTION";

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "contract_amount")
    private BigDecimal contractAmount;

    @Column(name = "progress_rate")
    private Integer progressRate = 0;

    @Column(name = "risk_level", length = 10)
    private String riskLevel = "보통";

    @Column(name = "team", length = 100)
    private String team;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
