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
@Table(name = "pms_methodology_activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MethodologyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long id;

    @Column(name = "phase_id", nullable = false)
    private Long phaseId;

    @Column(name = "activity_code", nullable = false, length = 10)
    private String activityCode;

    @Column(name = "activity_name", nullable = false, length = 100)
    private String activityName;

    @Column(name = "activity_name_en", length = 100)
    private String activityNameEn;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
