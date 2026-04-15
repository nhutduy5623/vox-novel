package com.voxnovel.identity_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
