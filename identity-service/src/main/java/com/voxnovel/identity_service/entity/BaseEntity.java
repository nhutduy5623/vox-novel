package com.voxnovel.identity_service.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.time.LocalDateTime;

@MappedSuperclass
@Data
public class BaseEntity<T, ID> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private ID id; // Dùng Generic ID để linh hoạt Long, String, UUID...
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
