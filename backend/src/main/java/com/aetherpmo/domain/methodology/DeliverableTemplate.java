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
@Table(name = "pms_deliverable_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliverableTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deliverable_template_id")
    private Long id;

    @Column(name = "task_template_id", nullable = false)
    private Long taskTemplateId;

    @Column(name = "seq_no", nullable = false)
    private Integer seqNo;

    @Column(name = "deliverable_name", nullable = false, length = 300)
    private String deliverableName;

    @Column(name = "deliverable_category", length = 100)
    private String deliverableCategory;

    @Column(name = "stage", length = 20)
    private String stage;

    @Column(name = "version_no", length = 20)
    private String versionNo = "1.0";

    @Column(name = "description")
    private String description;

    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false;

    @Column(name = "default_selected", nullable = false)
    private Boolean defaultSelected = true;

    @Column(name = "template_file_ref", length = 200)
    private String templateFileRef;

    @Column(name = "file_name", length = 300)
    private String fileName;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
