package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;

import java.time.Instant;

import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng Repository điều phối giao dịch, tra cứu tài khoản nội bộ và hoàn tất Stripe PaymentSheet.
 */
public interface TransactionRepository {

    Single<AppResult<AccountLookupResult>> lookupInternalAccount(String accountNumber);

    Single<AppResult<TransactionDraft>> initiateTransaction(TransactionInitiateRequest request);

    Single<AppResult<TransactionConfirmResult>> confirmTransaction(String draftId, String otp, boolean biometricPassed);

    Single<AppResult<Instant>> resendOtp(String draftId);

    Single<AppResult<PaymentFinalizeResult>> finalizeStripePayment(String draftId);

    Single<AppResult<Transaction>> getTransactionById(String txId);

    Single<AppResult<Boolean>> checkServerHealth();
}
