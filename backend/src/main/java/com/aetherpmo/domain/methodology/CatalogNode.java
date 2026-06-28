package com.aetherpmo.domain.methodology;

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
@Table(name = "pms_catalog_node")
@Getter
@Setter
@NoArgsConstructor
public class CatalogNode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "node_id")
    private Long id;

    @Column(name = "parent_node_id")
    private Long parentNodeId;

    @Column(name = "node_type", nullable = false, length = 20)
    private String nodeType;

    @Column(name = "code", length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 300)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "seq_no")
    private Integer seqNo;

    @Column(name = "deliverable_category", length = 100)
    private String deliverableCategory;

    @Column(name = "stage", length = 20)
    private String stage;

    @Column(name = "template_file_ref", length = 200)
    private String templateFileRef;

    // template_tags(JSONB) 컬럼은 현재 미사용 — JPA 매핑에서 제외(컬럼은 DB에 존재, null 유지)
}
