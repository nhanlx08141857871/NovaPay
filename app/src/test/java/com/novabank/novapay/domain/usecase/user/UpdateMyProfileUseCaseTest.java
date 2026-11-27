package com.novabank.novapay.domain.usecase.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.repository.UserRepository;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class UpdateMyProfileUseCaseTest {

    private final UserRepository stubRepository = new UserRepository() {
        @Override
        public Observable<AppResult<User>> observeMyProfile() {
            return Observable.empty();
        }

        @Override
        public Single<AppResult<User>> getMyProfile() {
            return Single.never();
        }

        @Override
        public Single<AppResult<User>> getUserById(String uid) {
            return Single.never();
        }

        @Override
        public Single<AppResult<Void>> updateMyProfile(String phone, String address, String avatarUrl) {
            return Single.just(AppResult.success(null));
        }
    };

    private final UpdateMyProfileUseCase useCase = new UpdateMyProfileUseCase(stubRepository);

    @Test
    public void execute_validInputsWithinFirestoreRulesBounds_succeeds() {
        AppResult<Void> result = useCase.execute(
                " 0901234567 ",
                " 19 Nguyễn Hữu Thọ, Quận 7, TP.HCM ",
                " https://cdn.novapay.vn/avatar.png "
        ).blockingGet();

        assertTrue(result.isSuccess());
    }

    @Test
    public void execute_invalidPhoneOrOversizedAddressOrAvatar_returnsValidationError() {
        AppResult<Void> shortPhone = useCase.execute("0901", "Quận 7", "").blockingGet();
        assertTrue(shortPhone.isError());
        assertEquals("INVALID_PHONE", shortPhone.getErrorCode());

        AppResult<Void> alphabeticPhone = useCase.execute("abcdefghij", "Quận 7", "").blockingGet();
        assertTrue(alphabeticPhone.isError());
        assertEquals("INVALID_PHONE", alphabeticPhone.getErrorCode());

        AppResult<Void> emptyAddress = useCase.execute("0901234567", "   ", "").blockingGet();
        assertTrue(emptyAddress.isError());
        assertEquals("INVALID_ADDRESS", emptyAddress.getErrorCode());

        AppResult<Void> longAddress = useCase.execute("0901234567", "A".repeat(501), "").blockingGet();
        assertTrue(longAddress.isError());
        assertEquals("INVALID_ADDRESS", longAddress.getErrorCode());

        AppResult<Void> badSchemeAvatar = useCase.execute("0901234567", "Quận 7", "javascript:alert(1)").blockingGet();
        assertTrue(badSchemeAvatar.isError());
        assertEquals("INVALID_AVATAR", badSchemeAvatar.getErrorCode());

        AppResult<Void> longAvatar = useCase.execute("0901234567", "Quận 7", "https://" + "h".repeat(2045)).blockingGet();
        assertTrue(longAvatar.isError());
        assertEquals("INVALID_AVATAR", longAvatar.getErrorCode());
    }
}
