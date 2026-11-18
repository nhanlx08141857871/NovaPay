package com.novabank.novapay.domain.usecase;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.account.MortgageInfo;
import com.novabank.novapay.domain.model.bank.Beneficiary;
import com.novabank.novapay.domain.model.transaction.AccountLookupResult;
import com.novabank.novapay.domain.model.transaction.PaymentFinalizeResult;
import com.novabank.novapay.domain.model.transaction.Transaction;
import com.novabank.novapay.domain.model.transaction.TransactionConfirmResult;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.model.utility.UtilityBill;
import com.novabank.novapay.domain.model.utility.UtilityBillStatus;
import com.novabank.novapay.domain.model.utility.UtilityBillType;
import com.novabank.novapay.domain.repository.BeneficiaryRepository;
import com.novabank.novapay.domain.repository.SourceAccountRepository;
import com.novabank.novapay.domain.repository.TransactionRepository;
import com.novabank.novapay.domain.repository.UtilityRepository;

import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MoneyUseCasesTest {

    @Test
    public void getActiveCheckingAccounts_filtersOutSavingMortgageAndFrozen() {
        Account checkingActive = makeAccount("c1", AccountType.CHECKING, AccountStatus.ACTIVE, null);
        Account checkingFrozen = makeAccount("c2", AccountType.CHECKING, AccountStatus.FROZEN, null);
        Account saving = makeAccount("s1", AccountType.SAVING, AccountStatus.ACTIVE, null);
        Account mortgage = makeAccount("m1", AccountType.MORTGAGE, AccountStatus.ACTIVE,
                new MortgageInfo(500_000_000L, 400_000_000L, 60, 8.5, MortgageInfo.FREQUENCY_MONTHLY, 10_000_000L, Instant.now()));

        SourceAccountRepository repo = new SourceAccountRepository() {
            @Override
            public Observable<AppResult<List<Account>>> observeActiveCheckingAccounts() {
                return Observable.just(AppResult.success(Arrays.asList(checkingActive, checkingFrozen, saving, mortgage)));
            }

            @Override
            public Single<AppResult<List<Account>>> getActiveCheckingAccounts() {
                return Single.just(AppResult.success(Arrays.asList(checkingActive, checkingFrozen, saving)));
            }

            @Override
            public Single<AppResult<List<Account>>> getActiveMortgageAccounts() {
                return Single.just(AppResult.success(Arrays.asList(checkingActive, mortgage)));
            }

            @Override
            public Single<AppResult<Account>> getAccountById(String accountId) {
                if ("m1".equals(accountId)) return Single.just(AppResult.success(mortgage));
                if ("c1".equals(accountId)) return Single.just(AppResult.success(checkingActive));
                return Single.just(AppResult.error("ACCOUNT_NOT_FOUND", "Not found"));
            }
        };

        GetActiveCheckingAccountsUseCase checkingUc = new GetActiveCheckingAccountsUseCase(repo);
        List<Account> observed = checkingUc.observe().blockingFirst().getData();
        assertEquals(1, observed.size());
        assertEquals("c1", observed.get(0).getAccountId());

        GetMortgageAccountUseCase mortgageUc = new GetMortgageAccountUseCase(repo);
        assertEquals(1, mortgageUc.getActiveMortgages().blockingGet().getData().size());
        assertTrue(mortgageUc.getMortgageById("m1").blockingGet().isSuccess());
        assertEquals("INVALID_ARGUMENT", mortgageUc.getMortgageById("c1").blockingGet().getErrorCode());
    }

    @Test
    public void lookupLookupAccountAndUtilityBills_validateBlankInputs() {
        TransactionRepository txRepo = new TransactionRepository() {
            @Override
            public Single<AppResult<AccountLookupResult>> lookupInternalAccount(String accountNumber) {
                return Single.just(AppResult.success(new AccountLookupResult("acc-2", accountNumber, "Trần Bình")));
            }

            @Override
            public Single<AppResult<TransactionDraft>> initiateTransaction(TransactionInitiateRequest request) {
                return Single.never();
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
        };

        LookupInternalAccountUseCase lookupUc = new LookupInternalAccountUseCase(txRepo);
        assertEquals("INVALID_ARGUMENT", lookupUc.execute("   ").blockingGet().getErrorCode());
        assertEquals("INVALID_ARGUMENT", lookupUc.execute("1000 2000").blockingGet().getErrorCode());
        assertEquals("Trần Bình", lookupUc.execute(" 1000222333 ").blockingGet().getData().getOwnerName());

        UtilityRepository utilityRepo = new UtilityRepository() {
            @Override
            public Single<AppResult<List<UtilityBill>>> lookupUnpaidBills(String customerCode, UtilityBillType type) {
                return Single.just(AppResult.success(Collections.singletonList(
                        new UtilityBill("b1", UtilityBillType.ELECTRIC, "EVN HCMC", customerCode, "Lê An", "11/2026", 450_000L, Instant.now(), UtilityBillStatus.UNPAID)
                )));
            }

            @Override
            public Single<AppResult<List<ServiceItem>>> getServiceCatalog(ServiceCategory category) {
                return Single.just(AppResult.success(Collections.emptyList()));
            }

            @Override
            public Observable<AppResult<List<Order>>> observeMyOrders() {
                return Observable.just(AppResult.success(Collections.emptyList()));
            }
        };

        LookupUtilityBillsUseCase billUc = new LookupUtilityBillsUseCase(utilityRepo);
        assertEquals("INVALID_ARGUMENT", billUc.execute("", UtilityBillType.ELECTRIC).blockingGet().getErrorCode());
        assertEquals(450_000L, billUc.execute("PE0100123", UtilityBillType.ELECTRIC).blockingGet().getData().get(0).getAmount());
    }

    @Test
    public void saveBeneficiaryUseCase_validatesNameAndAccountNumber() {
        BeneficiaryRepository repo = new BeneficiaryRepository() {
            @Override
            public Observable<AppResult<List<Beneficiary>>> observeBeneficiaries() {
                return Observable.just(AppResult.success(Collections.emptyList()));
            }

            @Override
            public Single<AppResult<Beneficiary>> saveBeneficiary(Beneficiary beneficiary) {
                return Single.just(AppResult.success(beneficiary));
            }

            @Override
            public Single<AppResult<Boolean>> deleteBeneficiary(String beneficiaryId) {
                return Single.just(AppResult.success(Boolean.TRUE));
            }
        };

        SaveBeneficiaryUseCase saveUc = new SaveBeneficiaryUseCase(repo);
        assertEquals("INVALID_ARGUMENT", saveUc.execute(new Beneficiary("", "", "10001234", "VCB", "")).blockingGet().getErrorCode());
        assertTrue(saveUc.execute(new Beneficiary("", "Nguyen A", "10001234", "VCB", "Ban than")).blockingGet().isSuccess());
    }

    private static Account makeAccount(String id, AccountType type, AccountStatus status, MortgageInfo mortgage) {
        return new Account(
                id,
                "uid-1",
                type,
                "1000" + id,
                status,
                5_000_000L,
                "VND",
                10_000_000L,
                50_000_000L,
                null,
                mortgage,
                Instant.now()
        );
    }
}
