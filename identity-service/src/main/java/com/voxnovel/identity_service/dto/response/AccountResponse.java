package com.voxnovel.identity_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    Long id; // Khai báo trực tiếp ở đây
    String username;
    String status;
    LocalDateTime createdAt;
}
