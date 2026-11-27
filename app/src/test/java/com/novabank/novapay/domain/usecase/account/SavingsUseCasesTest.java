package com.novabank.novapay.domain.usecase.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.account.OpenSavingResult;
import com.novabank.novapay.domain.model.account.SettleSavingResult;
import com.novabank.novapay.domain.repository.SavingsRepository;

import org.junit.Test;

import java.time.Instant;

import io.reactivex.rxjava3.core.Single;

public class SavingsUseCasesTest {

    private final SavingsRepository fakeRepo = new SavingsRepository() {
        @Override
        public Single<AppResult<OpenSavingResult>> openSavingAccount(
                String fromCheckingAccountId,
                int termMonths,
                long principalAmount,
                String pinOrNull
        ) {
            return Single.just(AppResult.success(new OpenSavingResult(
                    "sav_new",
                    "2001000111",
                    "tx_open_1",
                    6.0,
                    Instant.parse("2027-05-01T00:00:00Z")
            )));
        }

        @Override
        public Single<AppResult<SettleSavingResult>> settleSavingAccount(
                String savingAccountId,
                String toCheckingAccountId,
                String pinOrNull
        ) {
            return Single.just(AppResult.success(new SettleSavingResult(
                    "tx_settle_1",
                    5_000_000L,
                    25_000L,
                    5_025_000L,
                    true,
                    0.5
            )));
        }
    };

    @Test
    public void openSaving_validatesMinPrincipalTermAndPin() {
        OpenSavingAccountUseCase useCase = new OpenSavingAccountUseCase(fakeRepo);

        AppResult<OpenSavingResult> lowAmount = useCase.execute("chk_1", 6, 500_000L, null).blockingGet();
        assertTrue(lowAmount.isError());
        assertEquals("INVALID_ARGUMENT", lowAmount.getErrorCode());

        AppResult<OpenSavingResult> badPin = useCase.execute("chk_1", 6, 5_000_000L, "12ab").blockingGet();
        assertTrue(badPin.isError());
        assertEquals("PIN_INVALID", badPin.getErrorCode());

        AppResult<OpenSavingResult> valid = useCase.execute("chk_1", 6, 5_000_000L, "123456").blockingGet();
        assertTrue(valid.isSuccess());
        assertEquals("sav_new", valid.getData().getSavingAccountId());
    }

    @Test
    public void settleSaving_validatesIdsAndPin() {
        SettleSavingAccountUseCase useCase = new SettleSavingAccountUseCase(fakeRepo);

        AppResult<SettleSavingResult> missingTarget = useCase.execute("sav_1", "", null).blockingGet();
        assertTrue(missingTarget.isError());
        assertEquals("INVALID_ARGUMENT", missingTarget.getErrorCode());

        AppResult<SettleSavingResult> valid = useCase.execute("sav_1", "chk_1", "123456").blockingGet();
        assertTrue(valid.isSuccess());
        assertEquals(5_025_000L, valid.getData().getTotalPayoutAmount());
    }
}
