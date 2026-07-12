package com.secureiq.SecureIQ.department.model;

import com.secureiq.SecureIQ.common.entity.BaseEntity;
import com.secureiq.SecureIQ.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(sql = "UPDATE departments SET deleted = true, department_name = concat(department_name, '_deleted_', cast(extract(epoch from now()) as varchar)), department_code = concat(department_code, '_deleted_', cast(extract(epoch from now()) as varchar)) WHERE id = ?")
@SQLRestriction("deleted = false")
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name", nullable = false, unique = true)
    private String departmentName;

    @Column(name = "department_code", nullable = false, unique = true)
    private String departmentCode;

    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_id")
    private User hod;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
