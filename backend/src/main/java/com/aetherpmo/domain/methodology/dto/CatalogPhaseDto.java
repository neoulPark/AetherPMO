package com.aetherpmo.domain.methodology.dto;

import java.util.List;

public record CatalogPhaseDto(
        String phaseCode,
        String phaseName,
        List<CatalogActivityDto> activities
) {
}
