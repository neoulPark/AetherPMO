package com.aetherpmo.domain.methodology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogNodeRepository extends JpaRepository<CatalogNode, Long> {

    List<CatalogNode> findByParentNodeIdIsNullOrderBySortOrderAscIdAsc();

    List<CatalogNode> findByParentNodeIdOrderBySortOrderAscIdAsc(Long parentNodeId);

    List<CatalogNode> findByNodeType(String nodeType);
}
