package com.aetherpmo.domain.tailoring;

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

@Entity
@Table(name = "pms_project_tailoring")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTailoring extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tailoring_id")
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "task_template_id")
    private Long taskTemplateId;

    @Column(name = "deliverable_template_id")
    private Long deliverableTemplateId;

    @Column(name = "is_selected", nullable = false)
    private Boolean isSelected = true;

    @Column(name = "exclude_reason")
    private String excludeReason;

    @Column(name = "generated_task_id")
    private Long generatedTaskId;

    @Column(name = "generated_deliverable_id")
    private Long generatedDeliverableId;
}
