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
@Table(name = "pms_methodology_phase")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MethodologyPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "phase_id")
    private Long id;

    @Column(name = "phase_code", nullable = false, length = 10)
    private String phaseCode;

    @Column(name = "phase_name", nullable = false, length = 100)
    private String phaseName;

    @Column(name = "phase_name_en", length = 100)
    private String phaseNameEn;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
