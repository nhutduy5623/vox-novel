package com.voxnovel.identity_service.service;

import com.voxnovel.identity_service.dto.request.AccountCreationRequest;
import com.voxnovel.identity_service.dto.response.AccountResponse;
import com.voxnovel.identity_service.entity.Account;
import com.voxnovel.identity_service.exception.AppException;
import com.voxnovel.identity_service.exception.ErrorCode;
import com.voxnovel.identity_service.mapper.AccountMapper;
import com.voxnovel.identity_service.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {
    AccountRepository accountRepository;
    AccountMapper accountMapper;

    @Transactional
    public AccountResponse createAccount(AccountCreationRequest request) {
        // 1. Kiểm tra tồn tại
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // 2. Chuyển DTO sang Entity (Dùng Mapper)
        Account account = accountMapper.toAccount(request);

        // 3. Set trạng thái mặc định (Sau này sẽ băm password ở đây)
        account.setStatus("ACTIVE");

        // 4. Lưu và trả về DTO
        return accountMapper.toAccountResponse(accountRepository.save(account));
    }

    public List<AccountResponse> getAllAccounts() {
        log.info("Đang lấy danh sách tất cả Account...");
        return accountRepository.findAll().stream()
                .map(accountMapper::toAccountResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long id) {
        return accountRepository.findById(id)
                .map(accountMapper::toAccountResponse)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
