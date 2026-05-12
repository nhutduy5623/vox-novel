package com.voxnovel.identity_service.controller;

import com.voxnovel.identity_service.dto.request.AccountCreationRequest;
import com.voxnovel.identity_service.dto.response.AccountResponse;
import com.voxnovel.identity_service.dto.response.ApiResponse;
import com.voxnovel.identity_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {
    AccountService accountService;
    @PostMapping("/register")
    public ApiResponse<AccountResponse> register(@RequestBody @Valid AccountCreationRequest request) {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.createAccount(request))
                .message("Register Success!")
                .build();
    }
    @GetMapping
    public ApiResponse<List<AccountResponse>> getAll() {
        return ApiResponse.<List<AccountResponse>>builder()
                .result(accountService.getAllAccounts())
                .build();
    }
    @GetMapping("/{id}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable Long id) {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.getAccountById(id))
                .build();
    }
}
