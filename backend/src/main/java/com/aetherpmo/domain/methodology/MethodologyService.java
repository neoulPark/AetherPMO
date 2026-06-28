package com.aetherpmo.domain.methodology;

import com.aetherpmo.domain.methodology.dto.CatalogNodeDto;
import com.aetherpmo.domain.methodology.dto.CatalogNodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MethodologyService {

    private final CatalogNodeRepository nodeRepository;

    @Transactional(readOnly = true)
    public List<CatalogNodeDto> getCatalog() {
        return nodeRepository.findByParentNodeIdIsNullOrderBySortOrderAscIdAsc().stream()
                .map(this::toDtoTree)
                .toList();
    }

    private CatalogNodeDto toDtoTree(CatalogNode node) {
        List<CatalogNodeDto> children =
                nodeRepository.findByParentNodeIdOrderBySortOrderAscIdAsc(node.getId()).stream()
                        .map(this::toDtoTree)
                        .toList();
        return new CatalogNodeDto(
                node.getId(),
                node.getParentNodeId(),
                node.getNodeType(),
                node.getCode(),
                node.getName(),
                node.getIsOptional(),
                node.getSeqNo(),
                node.getSortOrder(),
                node.getWorkflowId(),
                children);
    }

    @Transactional
    public CatalogNodeDto createNode(CatalogNodeRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        CatalogNode node = new CatalogNode();
        node.setParentNodeId(req.parentNodeId());
        node.setNodeType(req.nodeType());
        node.setCode(req.code());
        node.setName(req.name());
        node.setDescription(req.description());
        if (req.isOptional() != null) node.setIsOptional(req.isOptional());
        if (req.sortOrder() != null) node.setSortOrder(req.sortOrder());
        node.setSeqNo(req.seqNo());
        node.setDeliverableCategory(req.deliverableCategory());
        node.setStage(req.stage());
        node.setWorkflowId(req.workflowId());
        validateHierarchy(req.nodeType(), req.parentNodeId());
        CatalogNode saved = nodeRepository.save(node);
        return toDtoTree(saved);
    }

    @Transactional
    public CatalogNodeDto updateNode(Long id, CatalogNodeRequest req) {
        CatalogNode node = nodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Catalog node not found: " + id));
        if (req.name() != null) {
            if (req.name().isBlank()) throw new IllegalArgumentException("name must not be blank");
            node.setName(req.name());
        }
        if (req.code() != null) node.setCode(req.code());
        if (req.isOptional() != null) node.setIsOptional(req.isOptional());
        if (req.sortOrder() != null) node.setSortOrder(req.sortOrder());
        if (req.description() != null) node.setDescription(req.description());
        if (req.seqNo() != null) node.setSeqNo(req.seqNo());
        if (req.deliverableCategory() != null) node.setDeliverableCategory(req.deliverableCategory());
        if (req.stage() != null) node.setStage(req.stage());
        if (req.workflowId() != null) node.setWorkflowId(req.workflowId());
        return toDtoTree(nodeRepository.save(node));
    }

    @Transactional
    public void deleteNode(Long id) {
        if (!nodeRepository.existsById(id)) {
            throw new IllegalArgumentException("Catalog node not found: " + id);
        }
        // DB FK ON DELETE CASCADE removes descendants.
        nodeRepository.deleteById(id);
    }

    // Lenient hierarchy sanity check: warn (no-op) rather than hard-fail,
    // except enforce PHASE has a null parent / non-PHASE has a parent.
    private void validateHierarchy(String nodeType, Long parentNodeId) {
        if (nodeType == null) return;
        boolean isPhase = "PHASE".equals(nodeType);
        if (isPhase && parentNodeId != null) {
            throw new IllegalArgumentException("PHASE node must not have a parent");
        }
        if (!isPhase && parentNodeId == null) {
            throw new IllegalArgumentException(nodeType + " node requires a parent node");
        }
    }
}
