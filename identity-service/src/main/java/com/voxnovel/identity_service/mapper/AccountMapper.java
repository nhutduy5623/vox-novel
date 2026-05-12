package com.voxnovel.identity_service.mapper;

import com.voxnovel.identity_service.dto.request.AccountCreationRequest;
import com.voxnovel.identity_service.dto.response.AccountResponse;
import com.voxnovel.identity_service.entity.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    Account toAccount(AccountCreationRequest request);
    // Chuyển từ Entity đã lưu thành Response trả về (Giấu password đi)
    AccountResponse toAccountResponse(Account account);
}