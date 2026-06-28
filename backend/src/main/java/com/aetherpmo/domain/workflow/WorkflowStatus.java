package com.aetherpmo.domain.workflow;

import com.aetherpmo.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pms_workflow_status")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "category", length = 20)
    private String category;

    @Column(name = "is_initial", nullable = false)
    private Boolean isInitial = false;

    @Column(name = "is_final", nullable = false)
    private Boolean isFinal = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
