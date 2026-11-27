package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.SetTransactionPinRequestDto;
import com.novabank.novapay.data.dto.UpdateAccountControlsRequestDto;
import com.novabank.novapay.data.mapper.ApiErrorParser;
import com.novabank.novapay.data.mapper.SecurityMapper;
import com.novabank.novapay.data.remote.server.SecurityApi;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.security.AccountControlResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.repository.SecurityRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class SecurityRepositoryImpl implements SecurityRepository {

    private final SecurityApi securityApi;
    private final ApiErrorParser apiErrorParser;

    @Inject
    public SecurityRepositoryImpl(SecurityApi securityApi, ApiErrorParser apiErrorParser) {
        this.securityApi = securityApi;
        this.apiErrorParser = apiErrorParser;
    }

    @Override
    public Single<AppResult<SecurityStatus>> getSecurityStatus() {
        return securityApi.getSecurityStatus()
                .map(dto -> AppResult.success(SecurityMapper.toDomain(dto)))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<SecurityStatus>> setTransactionPin(String currentPinOrNull, String newPin) {
        return securityApi.setTransactionPin(new SetTransactionPinRequestDto(currentPinOrNull, newPin))
                .map(dto -> AppResult.success(SecurityMapper.toDomain(dto)))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<AccountControlResult>> updateCheckingControls(
            String accountId,
            AccountStatus statusOrNull,
            Long perTxnLimitOrNull,
            Long dailyLimitOrNull,
            String pinOrNull
    ) {
        String statusStr = statusOrNull != null ? statusOrNull.name() : null;
        return securityApi.updateAccountControls(
                        accountId,
                        new UpdateAccountControlsRequestDto(statusStr, perTxnLimitOrNull, dailyLimitOrNull, pinOrNull)
                )
                .map(dto -> AppResult.success(SecurityMapper.toDomain(dto)))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }
}
