package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.ExternalRecipient;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.repository.TransactionRepository;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;

import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InitiateTransactionUseCaseTest {

    private FakeTransactionRepository fakeRepository;
    private InitiateTransactionUseCase useCase;

    @Before
    public void setUp() {
        fakeRepository = new FakeTransactionRepository();
        useCase = new InitiateTransactionUseCase(fakeRepository);
    }

    @Test
    public void rejectsNonCheckingOrInactiveSourceAccounts() {
        Account saving = account("acc-sav", AccountType.SAVING, AccountStatus.ACTIVE, 10_000_000L, 5_000_000L);
        Account mortgage = account("acc-mor", AccountType.MORTGAGE, AccountStatus.ACTIVE, 10_000_000L, 5_000_000L);
        Account frozen = account("acc-frz", AccountType.CHECKING, AccountStatus.FROZEN, 10_000_000L, 5_000_000L);

        AppResult<TransactionDraft> r1 = useCase.execute(internalReq("acc-sav", "acc-dst", 100_000L), saving).blockingGet();
        AppResult<TransactionDraft> r2 = useCase.execute(internalReq("acc-mor", "acc-dst", 100_000L), mortgage).blockingGet();
        AppResult<TransactionDraft> r3 = useCase.execute(internalReq("acc-frz", "acc-dst", 100_000L), frozen).blockingGet();

        assertTrue(r1.isError());
        assertEquals("ACCOUNT_NOT_ACTIVE", r1.getErrorCode());
        assertTrue(r2.isError());
        assertEquals("ACCOUNT_NOT_ACTIVE", r2.getErrorCode());
        assertTrue(r3.isError());
        assertEquals("ACCOUNT_NOT_ACTIVE", r3.getErrorCode());
        assertNull(fakeRepository.lastInitiatedRequest);
    }

    @Test
    public void rejectsSelfTransferInternal() {
        Account checking = account("acc-1", AccountType.CHECKING, AccountStatus.ACTIVE, 10_000_000L, 20_000_000L);

        AppResult<TransactionDraft> result = useCase.execute(
                internalReq("acc-1", "acc-1", 200_000L),
                checking
        ).blockingGet();

        assertTrue(result.isError());
        assertEquals("INVALID_ARGUMENT", result.getErrorCode());
        assertNull(fakeRepository.lastInitiatedRequest);
    }

    @Test
    public void rejectsInsufficientFundsAndLimitExceededForDebitTransactions() {
        Account checking = account("acc-1", AccountType.CHECKING, AccountStatus.ACTIVE, 1_000_000L, 5_000_000L);

        AppResult<TransactionDraft> overBalance = useCase.execute(
                internalReq("acc-1", "acc-2", 2_000_000L),
                checking
        ).blockingGet();
        assertTrue(overBalance.isError());
        assertEquals("INSUFFICIENT_FUNDS", overBalance.getErrorCode());

        AppResult<TransactionDraft> overLimit = useCase.execute(
                internalReq("acc-1", "acc-2", 6_000_000L),
                checking
        ).blockingGet();
        assertTrue(overLimit.isError());
        assertEquals("LIMIT_EXCEEDED", overLimit.getErrorCode());
        assertNull(fakeRepository.lastInitiatedRequest);
    }

    @Test
    public void allowsGatewayDepositAboveCheckingBalanceAndPerTxnLimit() {
        Account checking = account("acc-1", AccountType.CHECKING, AccountStatus.ACTIVE, 50_000L, 1_000_000L);
        TransactionInitiateRequest gatewayReq = new TransactionInitiateRequest(
                TransactionType.GATEWAY,
                "acc-1",
                null,
                null,
                8_000_000L,
                "Nạp tiền Stripe",
                TransactionMeta.forGateway("stripe")
        );

        AppResult<TransactionDraft> result = useCase.execute(gatewayReq, checking).blockingGet();

        assertTrue(result.isSuccess());
        assertNotNull(fakeRepository.lastInitiatedRequest);
        assertEquals(TransactionType.GATEWAY, fakeRepository.lastInitiatedRequest.getType());
        assertEquals(8_000_000L, fakeRepository.lastInitiatedRequest.getAmount());
    }

    @Test
    public void rejectsMortgagePaymentWithoutMortgageAccountId() {
        Account checking = account("acc-1", AccountType.CHECKING, AccountStatus.ACTIVE, 10_000_000L, 20_000_000L);
        TransactionInitiateRequest req = new TransactionInitiateRequest(
                TransactionType.MORTGAGE_PAYMENT,
                "acc-1",
                null,
                null,
                2_500_000L,
                "Trả nợ kỳ tháng 11",
                TransactionMeta.empty()
        );

        AppResult<TransactionDraft> result = useCase.execute(req, checking).blockingGet();
        assertTrue(result.isError());
        assertEquals("INVALID_ARGUMENT", result.getErrorCode());
    }

    @Test
    public void validatesInterbankAndDelegatesValidTransfer() {
        Account checking = account("acc-1", AccountType.CHECKING, AccountStatus.ACTIVE, 15_000_000L, 20_000_000L);
        TransactionInitiateRequest invalidExt = new TransactionInitiateRequest(
                TransactionType.TRANSFER_INTERBANK,
                "acc-1",
                null,
                new ExternalRecipient("", "999888777", "Nguyen A"),
                500_000L,
                "Chuyển liên ngân hàng",
                null
        );
        assertEquals("INVALID_ARGUMENT", useCase.execute(invalidExt, checking).blockingGet().getErrorCode());

        TransactionInitiateRequest validExt = new TransactionInitiateRequest(
                TransactionType.TRANSFER_INTERBANK,
                "acc-1",
                null,
                new ExternalRecipient("VCB", "999888777", "Nguyen A"),
                6_000_000L,
                "Chuyển liên ngân hàng",
                null
        );
        AppResult<TransactionDraft> ok = useCase.execute(validExt, checking).blockingGet();
        assertTrue(ok.isSuccess());
        assertEquals("draft-123", ok.getData().getDraftId());
    }

    private static TransactionInitiateRequest internalReq(String fromId, String toId, long amount) {
        return new TransactionInitiateRequest(
                TransactionType.TRANSFER_INTERNAL,
                fromId,
                toId,
                null,
                amount,
                "Chuyển tiền nội bộ",
                null
        );
    }

    private static Account account(
            String id,
            AccountType type,
            AccountStatus status,
            long balance,
            long perTxnLimit
    ) {
        return new Account(
                id,
                "uid-1",
                type,
                "1000123456",
                status,
                balance,
                "VND",
                perTxnLimit,
                50_000_000L,
                null,
                null,
                Instant.now()
        );
    }

    private static final class FakeTransactionRepository implements TransactionRepository {
        TransactionInitiateRequest lastInitiatedRequest;

        @Override
        public Single<AppResult<AccountLookupResult>> lookupInternalAccount(String accountNumber) {
            return Single.never();
        }

        @Override
        public Single<AppResult<TransactionDraft>> initiateTransaction(TransactionInitiateRequest request) {
            this.lastInitiatedRequest = request;
            return Single.just(AppResult.success(new TransactionDraft(
                    "draft-123",
                    request.getAmount() > 5_000_000L && request.getType() != TransactionType.GATEWAY,
                    request.getAmount() > 5_000_000L && request.getType() != TransactionType.GATEWAY,
                    null
            )));
        }

        @Override
        public Single<AppResult<TransactionConfirmResult>> confirmTransaction(String draftId, String otp, boolean biometricPassed) {
            return Single.never();
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
