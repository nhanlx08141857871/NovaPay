package com.novabank.novapay.domain.usecase.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.security.BiometricQuickLoginStore;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;
import com.novabank.novapay.domain.model.security.AccountControlResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.repository.AuthRepository;
import com.novabank.novapay.domain.repository.SecurityRepository;

import org.junit.Test;

import java.time.Instant;

import io.reactivex.rxjava3.core.Single;

public class SecurityUseCasesTest {

    private final SecurityRepository fakeSecurityRepo = new SecurityRepository() {
        @Override
        public Single<AppResult<SecurityStatus>> getSecurityStatus() {
            return Single.just(AppResult.success(new SecurityStatus(true, null)));
        }

        @Override
        public Single<AppResult<SecurityStatus>> setTransactionPin(String currentPinOrNull, String newPin) {
            return Single.just(AppResult.success(new SecurityStatus(true, null)));
        }

        @Override
        public Single<AppResult<AccountControlResult>> updateCheckingControls(
                String accountId,
                AccountStatus statusOrNull,
                Long perTxnLimitOrNull,
                Long dailyLimitOrNull,
                String pinOrNull
        ) {
            return Single.just(AppResult.success(new AccountControlResult(
                    statusOrNull != null ? statusOrNull : AccountStatus.ACTIVE,
                    perTxnLimitOrNull != null ? perTxnLimitOrNull : 10_000_000L,
                    dailyLimitOrNull != null ? dailyLimitOrNull : 50_000_000L
            )));
        }
    };

    private final AuthRepository fakeAuthRepo = new AuthRepository() {
        @Override
        public Single<AppResult<UserRole>> signIn(String email, String password) {
            return Single.just(AppResult.success(UserRole.CUSTOMER));
        }

        @Override
        public Single<AppResult<UserRole>> refreshRole() {
            return Single.just(AppResult.success(UserRole.CUSTOMER));
        }

        @Override
        public void signOut() {
        }

        @Override
        public boolean isSignedIn() {
            return true;
        }

        @Override
        public String getCurrentUid() {
            return "u1";
        }

        @Override
        public Single<AppResult<Boolean>> changePassword(String currentPassword, String newPassword) {
            return Single.just(AppResult.success(Boolean.TRUE));
        }

        @Override
        public Single<AppResult<Boolean>> sendPasswordResetEmail(String email) {
            return Single.just(AppResult.success(Boolean.TRUE));
        }
    };

    @Test
    public void setTransactionPin_enforcesSixDigitsAndDifferentPinWhenChanging() {
        SetTransactionPinUseCase useCase = new SetTransactionPinUseCase(fakeSecurityRepo);

        assertTrue(useCase.execute(false, null, "12345").blockingGet().isError());
        assertTrue(useCase.execute(true, "123456", "123456").blockingGet().isError());
        assertTrue(useCase.execute(true, "123456", "654321").blockingGet().isSuccess());
    }

    @Test
    public void updateCheckingControls_enforcesRangeAndPerTxnNotAboveDaily() {
        UpdateCheckingControlsUseCase useCase = new UpdateCheckingControlsUseCase(fakeSecurityRepo);

        AppResult<AccountControlResult> belowMin =
                useCase.execute("chk_1", AccountStatus.ACTIVE, 500_000L, 10_000_000L, null).blockingGet();
        assertTrue(belowMin.isError());

        AppResult<AccountControlResult> perTxnExceedsDaily =
                useCase.execute("chk_1", AccountStatus.ACTIVE, 60_000_000L, 50_000_000L, null).blockingGet();
        assertTrue(perTxnExceedsDaily.isError());

        AppResult<AccountControlResult> validFreeze =
                useCase.execute("chk_1", AccountStatus.FROZEN, 10_000_000L, 50_000_000L, "123456").blockingGet();
        assertTrue(validFreeze.isSuccess());
        assertEquals(AccountStatus.FROZEN, validFreeze.getData().getStatus());
    }

    @Test
    public void changePasswordAndResetEmail_validateInputs() {
        ChangePasswordUseCase changeUseCase = new ChangePasswordUseCase(fakeAuthRepo);
        assertTrue(changeUseCase.execute("old123", "123").blockingGet().isError());
        assertTrue(changeUseCase.execute("old123", "old123").blockingGet().isError());
        assertTrue(changeUseCase.execute("old123", "newPass123").blockingGet().isSuccess());

        SendPasswordResetEmailUseCase resetUseCase = new SendPasswordResetEmailUseCase(fakeAuthRepo);
        assertTrue(resetUseCase.execute("not-an-email").blockingGet().isError());
        assertTrue(resetUseCase.execute("customer@example.com").blockingGet().isSuccess());
    }

    @Test
    public void buildPatch_omitsLimitsOnEmergencyFreezeAndUnchangedFields() {
        Account legacyActive = new Account(
                "chk_1",
                "u1",
                AccountType.CHECKING,
                "1001000001",
                AccountStatus.ACTIVE,
                5_000_000L,
                "VND",
                0L,
                0L,
                null,
                null,
                Instant.now()
        );

        // Emergency freeze with legacy out-of-range limits must omit limit fields and pass validation
        UpdateCheckingControlsUseCase.ControlsPatch freezePatch =
                UpdateCheckingControlsUseCase.buildPatch(legacyActive, true, 0L, 0L);
        assertTrue(freezePatch.hasChanges());
        assertEquals(AccountStatus.FROZEN, freezePatch.getStatusOrNull());
        assertNull(freezePatch.getPerTxnLimitOrNull());
        assertNull(freezePatch.getDailyLimitOrNull());

        UpdateCheckingControlsUseCase useCase = new UpdateCheckingControlsUseCase(fakeSecurityRepo);
        AppResult<AccountControlResult> freezeResult = useCase.execute(
                legacyActive.getAccountId(),
                freezePatch.getStatusOrNull(),
                freezePatch.getPerTxnLimitOrNull(),
                freezePatch.getDailyLimitOrNull(),
                null
        ).blockingGet();
        assertTrue(freezeResult.isSuccess());
        assertEquals(AccountStatus.FROZEN, freezeResult.getData().getStatus());

        // Unchanged status & limits -> hasChanges() == false, execute rejects empty patch
        Account standardActive = new Account(
                "chk_2",
                "u1",
                AccountType.CHECKING,
                "1001000002",
                AccountStatus.ACTIVE,
                10_000_000L,
                "VND",
                10_000_000L,
                50_000_000L,
                null,
                null,
                Instant.now()
        );
        UpdateCheckingControlsUseCase.ControlsPatch noOpPatch =
                UpdateCheckingControlsUseCase.buildPatch(standardActive, false, 10_000_000L, 50_000_000L);
        assertFalse(noOpPatch.hasChanges());
        assertTrue(useCase.execute("chk_2", null, null, null, null).blockingGet().isError());

        // Changing only perTxnLimit omits status and dailyLimit
        UpdateCheckingControlsUseCase.ControlsPatch perTxnOnly =
                UpdateCheckingControlsUseCase.buildPatch(standardActive, false, 20_000_000L, 50_000_000L);
        assertTrue(perTxnOnly.hasChanges());
        assertNull(perTxnOnly.getStatusOrNull());
        assertEquals(Long.valueOf(20_000_000L), perTxnOnly.getPerTxnLimitOrNull());
        assertNull(perTxnOnly.getDailyLimitOrNull());
    }

    @Test
    public void biometricQuickLoginStore_matchesEnrolledEmail_isolatesAccounts() {
        assertTrue(BiometricQuickLoginStore.matchesEnrolledEmail("user.a@example.com", "User.A@example.com "));
        assertFalse(BiometricQuickLoginStore.matchesEnrolledEmail("user.a@example.com", "user.b@example.com"));
        assertFalse(BiometricQuickLoginStore.matchesEnrolledEmail("user.a@example.com", null));
        assertFalse(BiometricQuickLoginStore.matchesEnrolledEmail(null, "user.b@example.com"));
        assertFalse(BiometricQuickLoginStore.matchesEnrolledEmail("", ""));
    }
}
