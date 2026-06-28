package com.aetherpmo.domain.methodology;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.methodology.dto.CatalogPhaseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/methodology")
@RequiredArgsConstructor
public class MethodologyController {

    private final MethodologyService methodologyService;

    @GetMapping("/catalog")
    public ApiResponse<List<CatalogPhaseDto>> catalog() {
        return ApiResponse.ok(methodologyService.getCatalog());
    }
}
