package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.officer.EkycReviewCandidate;
import com.novabank.novapay.domain.model.officer.OfficerDashboardSummary;
import com.novabank.novapay.domain.model.user.CustomerProfile;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface OfficerRepository {

    Single<AppResult<OfficerDashboardSummary>> getDashboardSummary();

    Observable<AppResult<List<CustomerProfile>>> observeCustomers();

    Single<AppResult<String>> createCustomer(
            String fullName,
            String email,
            String phone,
            String nationalId,
            String dob,
            String address,
            String initialPassword
    );

    Single<AppResult<Boolean>> updateCustomerProfile(
            String uid,
            String fullName,
            String phone,
            String address
    );

    Single<AppResult<Boolean>> updateCustomerStatus(String uid, String nextStatus);

    Single<AppResult<List<EkycReviewCandidate>>> getEkycReviewCandidates();

    Single<AppResult<String>> getEkycImageBase64(String uid);

    Single<AppResult<String>> reviewEkyc(String uid, String decision);

    Completable signOut();
}
