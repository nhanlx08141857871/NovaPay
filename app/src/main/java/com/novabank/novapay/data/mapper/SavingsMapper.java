package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.novabank.novapay.data.dto.OpenSavingResponseDto;
import com.novabank.novapay.data.dto.SettleSavingResponseDto;
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.model.account.SettleSavingResult;

import java.time.Instant;

public final class SavingsMapper {

    private SavingsMapper() {
    }

    @Nullable
    public static OpenSavingResult toDomain(@Nullable OpenSavingResponseDto dto) {
        if (dto == null || dto.getSavingAccountId() == null || dto.getSavingAccountId().trim().isEmpty()) {
            return null;
        }
        Instant maturityDate = dto.getMaturityDateEpochMs() > 0L
                ? Instant.ofEpochMilli(dto.getMaturityDateEpochMs())
                : null;
        return new OpenSavingResult(
                dto.getSavingAccountId(),
                dto.getAccountNumber(),
                dto.getTxId(),
                dto.getAnnualRate(),
                maturityDate
        );
    }

    @Nullable
    public static SettleSavingResult toDomain(@Nullable SettleSavingResponseDto dto) {
        if (dto == null || dto.getTxId() == null || dto.getTxId().trim().isEmpty()) {
            return null;
        }
        return new SettleSavingResult(
                dto.getTxId(),
                dto.getPrincipalAmount(),
                dto.getInterestAmount(),
                dto.getTotalPayoutAmount(),
                dto.isEarlySettlement(),
                dto.getAppliedAnnualRate()
        );
    }
}
