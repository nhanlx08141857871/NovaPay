package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.novabank.novapay.data.dto.SecurityStatusResponseDto;
import com.novabank.novapay.data.dto.SetTransactionPinResponseDto;
import com.novabank.novapay.data.dto.UpdateAccountControlsResponseDto;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.security.AccountControlResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;

import java.time.Instant;

public final class SecurityMapper {

    private SecurityMapper() {
    }

    public static SecurityStatus toDomain(@Nullable SecurityStatusResponseDto dto) {
        if (dto == null) {
            return new SecurityStatus(false, null);
        }
        Long lockedUntilMs = dto.getPinLockedUntilEpochMs();
        Instant lockedUntil = lockedUntilMs != null && lockedUntilMs > 0L
                ? Instant.ofEpochMilli(lockedUntilMs)
                : null;
        return new SecurityStatus(dto.isHasTransactionPin(), lockedUntil);
    }

    public static SecurityStatus toDomain(@Nullable SetTransactionPinResponseDto dto) {
        if (dto == null) {
            return new SecurityStatus(true, null);
        }
        return new SecurityStatus(dto.isHasTransactionPin(), null);
    }

    public static AccountControlResult toDomain(@Nullable UpdateAccountControlsResponseDto dto) {
        if (dto == null) {
            return new AccountControlResult(AccountStatus.ACTIVE, 0L, 0L);
        }
        return new AccountControlResult(
                parseStatus(dto.getStatus()),
                dto.getPerTxnLimit(),
                dto.getDailyLimit()
        );
    }

    private static AccountStatus parseStatus(@Nullable String raw) {
        if ("FROZEN".equalsIgnoreCase(raw)) {
            return AccountStatus.FROZEN;
        }
        if ("CLOSED".equalsIgnoreCase(raw)) {
            return AccountStatus.CLOSED;
        }
        return AccountStatus.ACTIVE;
    }
}
