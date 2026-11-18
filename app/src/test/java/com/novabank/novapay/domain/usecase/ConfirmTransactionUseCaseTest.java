package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.repository.TransactionRepository;

import org.junit.Test;

import java.time.Instant;

import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfirmTransactionUseCaseTest {

    @Test
    public void rejectsInvalidDraftIdAndNonSixDigitOtp() {
        StubRepo repo = new StubRepo();
        ConfirmTransactionUseCase useCase = new ConfirmTransactionUseCase(repo);

        assertEquals("INVALID_ARGUMENT", useCase.execute("  ", "123456", true).blockingGet().getErrorCode());
        assertEquals("OTP_INVALID", useCase.execute("draft-1", null, true).blockingGet().getErrorCode());
        assertEquals("OTP_INVALID", useCase.execute("draft-1", "12345", true).blockingGet().getErrorCode());
        assertEquals("OTP_INVALID", useCase.execute("draft-1", "1234567", true).blockingGet().getErrorCode());
        assertEquals("OTP_INVALID", useCase.execute("draft-1", "12a456", true).blockingGet().getErrorCode());
        assertNull(repo.capturedOtp);
    }

    @Test
    public void trimsAndDelegatesValidSixDigitOtp() {
        StubRepo repo = new StubRepo();
        ConfirmTransactionUseCase useCase = new ConfirmTransactionUseCase(repo);

        AppResult<TransactionConfirmResult> result = useCase.execute(" draft-99 ", " 482910 ", true).blockingGet();

        assertTrue(result.isSuccess());
        assertEquals("draft-99", repo.capturedDraftId);
        assertEquals("482910", repo.capturedOtp);
        assertTrue(repo.capturedBiometric);
        assertEquals("tx-99", result.getData().getTxId());
    }

    private static final class StubRepo implements TransactionRepository {
        String capturedDraftId;
        String capturedOtp;
        boolean capturedBiometric;

        @Override
        public Single<AppResult<AccountLookupResult>> lookupInternalAccount(String accountNumber) {
            return Single.never();
        }

        @Override
        public Single<AppResult<TransactionDraft>> initiateTransaction(TransactionInitiateRequest request) {
            return Single.never();
        }

        @Override
        public Single<AppResult<TransactionConfirmResult>> confirmTransaction(String draftId, String otp, boolean biometricPassed) {
            this.capturedDraftId = draftId;
            this.capturedOtp = otp;
            this.capturedBiometric = biometricPassed;
            return Single.just(AppResult.success(new TransactionConfirmResult("tx-99", "SUCCESS", null)));
        }

        @Override
        public Single<AppResult<Instant>> resendOtp(String draftId) {
            return Single.never();
        }

        @Override
        public Single<AppResult<PaymentFinalizeResult>> finalizeStripePayment(String draftId) {
            return Single.never();
        }

        @Override
        public Single<AppResult<Transaction>> getTransactionById(String txId) {
            return Single.never();
        }

        @Override
        public Single<AppResult<Boolean>> checkServerHealth() {
            return Single.never();
        }
    }
}
