package com.aetherpmo.domain.methodology;

import com.aetherpmo.common.ApiResponse;
import com.aetherpmo.domain.methodology.dto.CatalogNodeDto;
import com.aetherpmo.domain.methodology.dto.CatalogNodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/methodology")
@RequiredArgsConstructor
public class MethodologyController {

    private final MethodologyService methodologyService;

    @GetMapping("/catalog")
    public ApiResponse<List<CatalogNodeDto>> catalog() {
        return ApiResponse.ok(methodologyService.getCatalog());
    }

    @PostMapping("/nodes")
    public ApiResponse<CatalogNodeDto> createNode(@RequestBody CatalogNodeRequest request) {
        return ApiResponse.ok(methodologyService.createNode(request), "Catalog node created");
    }

    @PutMapping("/nodes/{id}")
    public ApiResponse<CatalogNodeDto> updateNode(
            @PathVariable Long id, @RequestBody CatalogNodeRequest request) {
        return ApiResponse.ok(methodologyService.updateNode(id, request), "Catalog node updated");
    }

    @DeleteMapping("/nodes/{id}")
    public ApiResponse<Void> deleteNode(@PathVariable Long id) {
        methodologyService.deleteNode(id);
        return ApiResponse.ok(null, "Catalog node deleted");
    }
}
