package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.User;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface UserRepository {

    Observable<AppResult<User>> observeMyProfile();

    Single<AppResult<User>> getMyProfile();

    Single<AppResult<User>> getUserById(String uid);

    Single<AppResult<Void>> updateMyProfile(String phone, String address, String avatarUrl);
}
