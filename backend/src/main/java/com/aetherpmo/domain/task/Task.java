package com.aetherpmo.domain.task;

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
@Table(name = "pms_task")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long id;

    @Column(name = "parent_task_id")
    private Long parentTaskId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "task_name", nullable = false, length = 300)
    private String taskName;

    @Column(name = "status", length = 20)
    private String status = "TODO";

    @Column(name = "progress_rate")
    private Integer progressRate = 0;

    @Column(name = "assignee_id")
    private Long assigneeId;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "planned_effort")
    private BigDecimal plannedEffort;

    @Column(name = "actual_effort")
    private BigDecimal actualEffort;

    @Column(name = "depth")
    private Integer depth = 0;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
}
