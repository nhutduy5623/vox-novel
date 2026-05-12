package com.voxnovel.identity_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Role extends BaseEntity<Role, Long>{

    @Column(unique = true, nullable = false)
    private String name; // Ví dụ: ROLE_ADMIN, ROLE_VIP

    private String description;
}
