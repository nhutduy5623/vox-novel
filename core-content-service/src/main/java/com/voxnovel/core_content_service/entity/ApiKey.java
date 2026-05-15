package com.voxnovel.core_content_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "api_keys")
@Data
public class ApiKey {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider; // Key này của nhà cung cấp nào

    @Column(nullable = false, unique = true)
    private String keyValue; // Chuỗi mã API (VD: sk-xxxx...)

    // Trạng thái hoạt động. Nếu AI Engine báo lỗi hết tiền, tự động set = false
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private String createdBy; // Ai là người thêm key này
}
