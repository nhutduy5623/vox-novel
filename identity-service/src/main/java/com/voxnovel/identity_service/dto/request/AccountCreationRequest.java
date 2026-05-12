package com.voxnovel.identity_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountCreationRequest {
    @Size(min = 4, message = "USERNAME_INVALID") // Username ít nhất 4 ký tự
    String username;
    @Size(min = 8, message = "PASSWORD_INVALID") private // Password ít nhất 8 ký tự
    String password;

    String email;
}