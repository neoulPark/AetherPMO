package com.aetherpmo.domain.company;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Lightweight read-only lookup for company names. There is no Company JPA entity
 * in this slice, so we read the name directly via a native query.
 */
@Repository
@RequiredArgsConstructor
public class CompanyNameRepository {

    private final EntityManager em;

    public String findNameById(Long companyId) {
        if (companyId == null) {
            return null;
        }
        List<?> rows = em.createNativeQuery(
                        "SELECT company_name FROM pms_company WHERE company_id = :id")
                .setParameter("id", companyId)
                .getResultList();
        return rows.isEmpty() ? null : (String) rows.get(0);
    }
}
