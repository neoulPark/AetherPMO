package com.aetherpmo.domain.contact;

import com.aetherpmo.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pms_contact_point")
@Getter
@Setter
@NoArgsConstructor
public class ContactPoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "field", length = 100)
    private String field;

    @Column(name = "contact_type", nullable = false, length = 20)
    private String contactType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "note")
    private String note;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
