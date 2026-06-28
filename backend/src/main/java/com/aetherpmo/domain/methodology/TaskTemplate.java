package com.aetherpmo.domain.methodology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pms_task_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_template_id")
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "task_code", nullable = false, length = 20)
    private String taskCode;

    @Column(name = "task_name", nullable = false, length = 200)
    private String taskName;

    @Column(name = "description")
    private String description;

    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false;

    @Column(name = "default_selected", nullable = false)
    private Boolean defaultSelected = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
