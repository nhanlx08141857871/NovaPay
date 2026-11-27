package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.OpenSavingRequestDto;
import com.novabank.novapay.data.dto.SettleSavingRequestDto;
import com.novabank.novapay.data.mapper.ApiErrorParser;
import com.novabank.novapay.data.mapper.SavingsMapper;
import com.novabank.novapay.data.remote.server.SavingsApi;
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.model.account.SettleSavingResult;
import com.novabank.novapay.domain.repository.SavingsRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class SavingsRepositoryImpl implements SavingsRepository {

    private final SavingsApi savingsApi;
    private final ApiErrorParser apiErrorParser;

    @Inject
    public SavingsRepositoryImpl(SavingsApi savingsApi, ApiErrorParser apiErrorParser) {
        this.savingsApi = savingsApi;
        this.apiErrorParser = apiErrorParser;
    }

    @Override
    public Single<AppResult<OpenSavingResult>> openSavingAccount(
            String fromCheckingAccountId,
            int termMonths,
            long principalAmount,
            String pinOrNull
    ) {
        return savingsApi.openSaving(new OpenSavingRequestDto(
                        fromCheckingAccountId,
                        termMonths,
                        principalAmount,
                        pinOrNull
                ))
                .map(dto -> {
                    OpenSavingResult mapped = SavingsMapper.toDomain(dto);
                    if (mapped == null) {
                        return AppResult.<OpenSavingResult>error("INVALID_RESPONSE", "Phản hồi mở sổ tiết kiệm không hợp lệ.");
                    }
                    return AppResult.success(mapped);
                })
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<SettleSavingResult>> settleSavingAccount(
            String savingAccountId,
            String toCheckingAccountId,
            String pinOrNull
    ) {
        return savingsApi.settleSaving(
                        savingAccountId,
                        new SettleSavingRequestDto(toCheckingAccountId, pinOrNull)
                )
                .map(dto -> {
                    SettleSavingResult mapped = SavingsMapper.toDomain(dto);
                    if (mapped == null) {
                        return AppResult.<SettleSavingResult>error("INVALID_RESPONSE", "Phản hồi tất toán sổ tiết kiệm không hợp lệ.");
                    }
                    return AppResult.success(mapped);
                })
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }
}
