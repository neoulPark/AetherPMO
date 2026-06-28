package com.aetherpmo.domain.task.dto;

import com.aetherpmo.domain.methodology.CatalogNode;

public record TemplateDeliverableDto(
        String name,
        String templateFileRef,
        Integer seqNo
) {
    public static TemplateDeliverableDto from(CatalogNode n) {
        return new TemplateDeliverableDto(n.getName(), n.getTemplateFileRef(), n.getSeqNo());
    }
}
