package com.voxnovel.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Profile extends BaseEntity<Profile, Long>{
    // Liên kết 1-1 với Account
    // nullable = false đảm bảo mỗi Profile phải thuộc về một Account
    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private String email;

    @Column(name = "full_name")
    private String fullName;

    private LocalDate dob; // Ngày tháng năm sinh (Date of birth)

    private String gender; // MALE, FEMALE, OTHER

    @Column(name = "avatar_url")
    private String avatarUrl;
}
