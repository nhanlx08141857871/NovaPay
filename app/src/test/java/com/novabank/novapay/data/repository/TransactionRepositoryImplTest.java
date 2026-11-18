package com.novabank.novapay.data.repository;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.AccountLookupResponseDto;
import com.novabank.novapay.data.dto.ConfirmTransactionRequestDto;
import com.novabank.novapay.data.dto.ConfirmTransactionResponseDto;
import com.novabank.novapay.data.dto.DraftRequestDto;
import com.novabank.novapay.data.dto.FinalizePaymentResponseDto;
import com.novabank.novapay.data.dto.HealthResponseDto;
import com.novabank.novapay.data.dto.InitiateTransactionRequestDto;
import com.novabank.novapay.data.dto.InitiateTransactionResponseDto;
import com.novabank.novapay.data.dto.ResendOtpResponseDto;
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
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionStatus;
import com.novabank.novapay.domain.model.transaction.TransactionType;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TransactionRepositoryImplTest {

    private FakeAccountLookupApi lookupApi;
    private FakeTransactionApi transactionApi;
    private FakePaymentApi paymentApi;
    private TransactionRepositoryImpl repository;

    @Before
    public void setUp() {
        lookupApi = new FakeAccountLookupApi();
        transactionApi = new FakeTransactionApi();
        paymentApi = new FakePaymentApi();
        repository = new TransactionRepositoryImpl(
                lookupApi,
                transactionApi,
                paymentApi,
                txId -> Single.just(AppResult.success(new Transaction(
                        txId,
                        TransactionType.TRANSFER_INTERNAL,
                        "acc-1",
                        "acc-2",
                        null,
                        500_000L,
                        0L,
                        "Test",
                        TransactionStatus.SUCCESS,
                        false,
                        false,
                        true,
                        false,
                        TransactionMeta.empty(),
                        "uid-1",
                        Instant.now()
                ))),
                new ApiErrorParser(new Gson())
        );
    }

    @Test
    public void lookupInternalAccount_mapsSuccessAndHttpExceptionEnvelope() {
        lookupApi.nextResult = Single.just(new AccountLookupResponseDto("acc-2", "1000222", "Nguyễn B"));
        AppResult<AccountLookupResult> ok = repository.lookupInternalAccount("1000222").blockingGet();
        assertTrue(ok.isSuccess());
        assertEquals("Nguyễn B", ok.getData().getOwnerName());

        ResponseBody errBody = ResponseBody.create(
                "{\"error\":\"ACCOUNT_NOT_FOUND\",\"message\":\"Không tìm thấy số tài khoản.\"}",
                MediaType.get("application/json")
        );
        lookupApi.nextResult = Single.error(new HttpException(Response.error(404, errBody)));
        AppResult<AccountLookupResult> err = repository.lookupInternalAccount("9999").blockingGet();
        assertTrue(err.isError());
        assertEquals("ACCOUNT_NOT_FOUND", err.getErrorCode());
    }

    @Test
    public void initiateAndConfirmAndFinalize_mapDtosAndNetworkErrors() {
        long expiryMs = 1_763_440_180_000L;
        transactionApi.nextInitiate = Single.just(new InitiateTransactionResponseDto(
                "draft-1",
                false,
                false,
                new JsonPrimitive(expiryMs)
        ));
        TransactionInitiateRequest req = new TransactionInitiateRequest(
                TransactionType.TRANSFER_INTERNAL,
                "acc-1",
                "acc-2",
                null,
                500_000L,
                "Chuyển khoản",
                null
        );
        AppResult<TransactionDraft> draftRes = repository.initiateTransaction(req).blockingGet();
        assertTrue(draftRes.isSuccess());
        assertEquals("draft-1", draftRes.getData().getDraftId());
        assertEquals(Instant.ofEpochMilli(expiryMs), draftRes.getData().getOtpExpiresAt());

        // High-value (> 5M) returns requiresFace = true and null otpExpiresAt
        transactionApi.nextInitiate = Single.just(new InitiateTransactionResponseDto(
                "draft-2",
                true,
                true,
                JsonNull.INSTANCE
        ));
        AppResult<TransactionDraft> highValRes = repository.initiateTransaction(req).blockingGet();
        assertTrue(highValRes.isSuccess());
        assertTrue(highValRes.getData().isRequiresFace());
        assertNull(highValRes.getData().getOtpExpiresAt());

        // Resend OTP with ISO string
        transactionApi.nextResend = Single.just(new ResendOtpResponseDto(new JsonPrimitive("2026-11-18T05:00:00Z")));
        AppResult<Instant> resendRes = repository.resendOtp("draft-1").blockingGet();
        assertTrue(resendRes.isSuccess());
        assertEquals(Instant.parse("2026-11-18T05:00:00Z"), resendRes.getData());

        // Confirm GATEWAY returns PENDING_PAYMENT + clientSecret
        transactionApi.nextConfirm = Single.just(new ConfirmTransactionResponseDto(null, "PENDING_PAYMENT", "pi_secret_123"));
        AppResult<TransactionConfirmResult> confirmRes = repository.confirmTransaction("draft-gw", "123456", false).blockingGet();
        assertTrue(confirmRes.isSuccess());
        assertTrue(confirmRes.getData().isPendingPayment());
        assertEquals("pi_secret_123", confirmRes.getData().getClientSecret());

        // Finalize Stripe
        paymentApi.nextFinalize = Single.just(new FinalizePaymentResponseDto("tx-gw-1", "SUCCESS"));
        AppResult<PaymentFinalizeResult> finRes = repository.finalizeStripePayment("draft-gw").blockingGet();
        assertTrue(finRes.isSuccess());
        assertEquals("tx-gw-1", finRes.getData().getTxId());

        // IOException maps to NETWORK_ERROR
        transactionApi.nextHealth = Single.error(new IOException("offline"));
        AppResult<Boolean> healthRes = repository.checkServerHealth().blockingGet();
        assertTrue(healthRes.isError());
        assertEquals("NETWORK_ERROR", healthRes.getErrorCode());

        // Receipt delegation
        AppResult<Transaction> receiptRes = repository.getTransactionById("tx-777").blockingGet();
        assertTrue(receiptRes.isSuccess());
        assertEquals("tx-777", receiptRes.getData().getTxId());
    }

    private static final class FakeAccountLookupApi implements AccountLookupApi {
        Single<AccountLookupResponseDto> nextResult = Single.never();

        @Override
        public Single<AccountLookupResponseDto> lookupAccount(String accountNumber) {
            return nextResult;
        }
    }

    private static final class FakeTransactionApi implements TransactionApi {
        Single<InitiateTransactionResponseDto> nextInitiate = Single.never();
        Single<ConfirmTransactionResponseDto> nextConfirm = Single.never();
        Single<ResendOtpResponseDto> nextResend = Single.never();
        Single<HealthResponseDto> nextHealth = Single.just(new HealthResponseDto(true));

        @Override
        public Single<InitiateTransactionResponseDto> initiate(InitiateTransactionRequestDto body) {
            return nextInitiate;
        }

        @Override
        public Single<ConfirmTransactionResponseDto> confirm(ConfirmTransactionRequestDto body) {
            return nextConfirm;
        }

        @Override
        public Single<ResendOtpResponseDto> resendOtp(DraftRequestDto body) {
            return nextResend;
        }

        @Override
        public Single<HealthResponseDto> health() {
            return nextHealth;
        }
    }

    private static final class FakePaymentApi implements PaymentApi {
        Single<FinalizePaymentResponseDto> nextFinalize = Single.never();

        @Override
        public Single<FinalizePaymentResponseDto> finalizePayment(DraftRequestDto body) {
            return nextFinalize;
        }
    }
}
