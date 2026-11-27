package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.ConfirmTransactionRequestDto;
import com.novabank.novapay.data.dto.DraftRequestDto;
import com.novabank.novapay.data.mapper.TransactionMapper;
import com.novabank.novapay.data.remote.firebase.MoneyFirestoreDataSource;
import com.novabank.novapay.data.remote.server.AccountLookupApi;
import com.novabank.novapay.data.remote.server.ApiErrorParser;
import com.novabank.novapay.data.remote.server.PaymentApi;
import com.novabank.novapay.data.remote.server.TransactionApi;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.repository.TransactionRepository;

import java.time.Instant;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

@Singleton
public class TransactionRepositoryImpl implements TransactionRepository {

    private final AccountLookupApi accountLookupApi;
    private final TransactionApi transactionApi;
    private final PaymentApi paymentApi;
    private final Function<String, Single<AppResult<Transaction>>> receiptLoader;
    private final ApiErrorParser errorParser;

    @Inject
    public TransactionRepositoryImpl(
            AccountLookupApi accountLookupApi,
            TransactionApi transactionApi,
            PaymentApi paymentApi,
            MoneyFirestoreDataSource firestoreDataSource,
            ApiErrorParser errorParser
    ) {
        this(accountLookupApi, transactionApi, paymentApi, firestoreDataSource::getTransactionById, errorParser);
    }

    TransactionRepositoryImpl(
            AccountLookupApi accountLookupApi,
            TransactionApi transactionApi,
            PaymentApi paymentApi,
            Function<String, Single<AppResult<Transaction>>> receiptLoader,
            ApiErrorParser errorParser
    ) {
        this.accountLookupApi = accountLookupApi;
        this.transactionApi = transactionApi;
        this.paymentApi = paymentApi;
        this.receiptLoader = receiptLoader;
        this.errorParser = errorParser;
    }

    @Override
    public Single<AppResult<AccountLookupResult>> lookupInternalAccount(String accountNumber) {
        return accountLookupApi.lookupAccount(accountNumber)
                .map(dto -> {
                    AccountLookupResult mapped = TransactionMapper.toLookupResult(dto);
                    if (mapped == null) {
                        return AppResult.<AccountLookupResult>error("ACCOUNT_NOT_FOUND", "Không tìm thấy tài khoản.");
                    }
                    return AppResult.success(mapped);
                })
                .onErrorReturn(errorParser::parseError);
    }

    @Override
    public Single<AppResult<TransactionDraft>> initiateTransaction(TransactionInitiateRequest request) {
        return transactionApi.initiate(TransactionMapper.toRequestDto(request))
                .map(dto -> {
                    TransactionDraft draft = TransactionMapper.toDraft(dto);
                    if (draft == null) {
                        return AppResult.<TransactionDraft>error("INVALID_RESPONSE", "Phản hồi khởi tạo giao dịch không hợp lệ.");
                    }
                    return AppResult.success(draft);
                })
                .onErrorReturn(errorParser::parseError);
    }

    @Override
    public Single<AppResult<TransactionConfirmResult>> confirmTransaction(
            String draftId,
            String otp,
            boolean biometricPassed
    ) {
        return transactionApi.confirm(new ConfirmTransactionRequestDto(draftId, otp, biometricPassed))
                .map(dto -> {
                    TransactionConfirmResult result = TransactionMapper.toConfirmResult(dto);
                    if (result == null) {
                        return AppResult.<TransactionConfirmResult>error("INVALID_RESPONSE", "Phản hồi xác nhận giao dịch không hợp lệ.");
                    }
                    return AppResult.success(result);
                })
                .onErrorReturn(errorParser::parseError);
    }

    @Override
    public Single<AppResult<Instant>> resendOtp(String draftId) {
        return transactionApi.resendOtp(new DraftRequestDto(draftId))
                .map(dto -> {
                    Instant expiresAt = dto != null ? TransactionMapper.parseOtpExpiresAt(dto.getOtpExpiresAt()) : null;
                    if (expiresAt == null) {
                        return AppResult.<Instant>error("INVALID_RESPONSE", "Không nhận được thời hạn OTP mới.");
                    }
                    return AppResult.success(expiresAt);
                })
                .onErrorReturn(errorParser::parseError);
    }

    @Override
    public Single<AppResult<PaymentFinalizeResult>> finalizeStripePayment(String draftId) {
        return paymentApi.finalizePayment(new DraftRequestDto(draftId))
                .map(dto -> {
                    PaymentFinalizeResult result = TransactionMapper.toFinalizeResult(dto);
                    if (result == null || result.getTxId() == null) {
                        return AppResult.<PaymentFinalizeResult>error("PAYMENT_NOT_COMPLETED", "Không thể hoàn tất thanh toán.");
                    }
                    return AppResult.success(result);
                })
                .onErrorReturn(errorParser::parseError);
    }

    @Override
    public Single<AppResult<Transaction>> getTransactionById(String txId) {
        return receiptLoader.apply(txId);
    }

    @Override
    public Single<AppResult<Boolean>> checkServerHealth() {
        return transactionApi.health()
                .map(dto -> AppResult.success(dto != null && dto.isOk()))
                .onErrorReturn(errorParser::parseError);
    }
}
