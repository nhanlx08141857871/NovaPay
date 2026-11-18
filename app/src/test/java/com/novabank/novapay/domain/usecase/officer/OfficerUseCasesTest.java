package com.novabank.novapay.domain.usecase.officer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;
import com.novabank.novapay.domain.model.officer.OfficerDashboardSummary;
import com.novabank.novapay.domain.model.rate.InterestRate;
import com.novabank.novapay.domain.model.user.CustomerProfile;
import com.novabank.novapay.domain.repository.InterestRateRepository;
import com.novabank.novapay.domain.repository.OfficerRepository;
import com.novabank.novapay.domain.usecase.rate.SaveInterestRateUseCase;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

public class OfficerUseCasesTest {

    private static final class FakeOfficerRepository implements OfficerRepository {
        boolean createCalled = false;
        String lastStatusUid;
        String lastStatusValue;
        String lastReviewDecision;

        @Override
        public Single<AppResult<OfficerDashboardSummary>> getDashboardSummary() {
            return Single.just(AppResult.success(new OfficerDashboardSummary(
                    "officer@novapay.vn", 1, 0, 0, 0,
                    Collections.emptyList(), Collections.emptyList()
            )));
        }

        @Override
        public Observable<AppResult<List<CustomerProfile>>> observeCustomers() {
            return Observable.just(AppResult.success(Collections.emptyList()));
        }

        @Override
        public Single<AppResult<String>> createCustomer(
                String fullName,
                String email,
                String phone,
                String nationalId,
                String dob,
                String address,
                String initialPassword
        ) {
            createCalled = true;
            return Single.just(AppResult.success("created_uid_1"));
        }

        @Override
        public Single<AppResult<Boolean>> updateCustomerProfile(
                String uid,
                String fullName,
                String phone,
                String address
        ) {
            return Single.just(AppResult.success(true));
        }

        @Override
        public Single<AppResult<Boolean>> updateCustomerStatus(String uid, String nextStatus) {
            lastStatusUid = uid;
            lastStatusValue = nextStatus;
            return Single.just(AppResult.success(true));
        }

        @Override
        public Single<AppResult<List<EkycReviewCandidate>>> getEkycReviewCandidates() {
            return Single.just(AppResult.success(Collections.emptyList()));
        }

        @Override
        public Single<AppResult<String>> getEkycImageBase64(String uid) {
            return Single.just(AppResult.success(null));
        }

        @Override
        public Single<AppResult<String>> reviewEkyc(String uid, String decision) {
            lastReviewDecision = decision;
            return Single.just(AppResult.success(decision));
        }

        @Override
        public Completable signOut() {
            return Completable.complete();
        }
    }

    private static final class FakeInterestRateRepository implements InterestRateRepository {
        int lastTerm = -1;
        double lastRate = -1.0;

        @Override
        public Observable<AppResult<List<InterestRate>>> observeAllRates() {
            return Observable.just(AppResult.success(Collections.emptyList()));
        }

        @Override
        public Observable<AppResult<InterestRate>> observeRateForTerm(int termMonths) {
            return Observable.empty();
        }

        @Override
        public Observable<AppResult<List<InterestRate>>> observeInterestRates() {
            return Observable.just(AppResult.success(Collections.emptyList()));
        }

        @Override
        public Single<AppResult<String>> saveInterestRate(int termMonths, double annualRate) {
            lastTerm = termMonths;
            lastRate = annualRate;
            return Single.just(AppResult.success("term_" + termMonths));
        }
    }

    @Test
    public void createCustomerUseCase_rejectsInvalidInputWithoutCallingRepo() {
        FakeOfficerRepository fakeRepo = new FakeOfficerRepository();
        CreateCustomerUseCase useCase = new CreateCustomerUseCase(fakeRepo);

        TestObserver<AppResult<String>> observer = useCase.execute(
                "A", "bad-email", "0901234567", "079099001234", "1999-05-20", "Quận 7", "123456"
        ).test();

        observer.assertComplete();
        assertTrue(observer.values().get(0).isError());
        assertEquals("INVALID_ARGUMENT", observer.values().get(0).getErrorCode());
        assertFalse(fakeRepo.createCalled);
    }

    @Test
    public void createCustomerUseCase_delegatesValidInputToRepo() {
        FakeOfficerRepository fakeRepo = new FakeOfficerRepository();
        CreateCustomerUseCase useCase = new CreateCustomerUseCase(fakeRepo);

        TestObserver<AppResult<String>> observer = useCase.execute(
                "Nguyễn Văn An",
                "An@NovaPay.vn",
                "0901 234 567",
                "079099001234",
                "20/05/1999",
                "19 Nguyễn Hữu Thọ",
                "secret123"
        ).test();

        observer.assertComplete();
        assertTrue(observer.values().get(0).isSuccess());
        assertEquals("created_uid_1", observer.values().get(0).getData());
        assertTrue(fakeRepo.createCalled);
    }

    @Test
    public void toggleCustomerLockUseCase_switchesActiveToLocked() {
        FakeOfficerRepository fakeRepo = new FakeOfficerRepository();
        ToggleCustomerLockUseCase useCase = new ToggleCustomerLockUseCase(fakeRepo);
        CustomerProfile activeCustomer = new CustomerProfile(
                "u1", "CUSTOMER", "An", "an@novapay.vn", "0901234567", "079099001234",
                "1999-01-01", "HCM", "", Constants.USER_STATUS_ACTIVE,
                Constants.EKYC_STATUS_VERIFIED, null, null, "o1", null, null
        );

        TestObserver<AppResult<Boolean>> observer = useCase.execute(activeCustomer).test();
        observer.assertComplete();
        assertTrue(observer.values().get(0).isSuccess());
        assertEquals("u1", fakeRepo.lastStatusUid);
        assertEquals(Constants.USER_STATUS_LOCKED, fakeRepo.lastStatusValue);
    }

    @Test
    public void reviewEkycUseCase_rejectsInvalidDecisionAndAcceptsVerified() {
        FakeOfficerRepository fakeRepo = new FakeOfficerRepository();
        ReviewEkycUseCase useCase = new ReviewEkycUseCase(fakeRepo);

        TestObserver<AppResult<String>> invalidObs = useCase.execute("u1", "UNKNOWN").test();
        assertTrue(invalidObs.values().get(0).isError());

        TestObserver<AppResult<String>> validObs =
                useCase.execute("u1", Constants.EKYC_STATUS_VERIFIED).test();
        assertTrue(validObs.values().get(0).isSuccess());
        assertEquals(Constants.EKYC_STATUS_VERIFIED, fakeRepo.lastReviewDecision);
    }

    @Test
    public void saveInterestRateUseCase_validatesBoundsBeforeRepositoryCall() {
        FakeInterestRateRepository fakeRepo = new FakeInterestRateRepository();
        SaveInterestRateUseCase useCase = new SaveInterestRateUseCase(fakeRepo);

        TestObserver<AppResult<String>> invalidObs = useCase.execute(6, 0.0).test();
        assertTrue(invalidObs.values().get(0).isError());
        assertEquals(-1, fakeRepo.lastTerm);

        TestObserver<AppResult<String>> validObs = useCase.execute(12, 6.8).test();
        assertTrue(validObs.values().get(0).isSuccess());
        assertEquals(12, fakeRepo.lastTerm);
        assertEquals(6.8, fakeRepo.lastRate, 0.0001);
    }
}
