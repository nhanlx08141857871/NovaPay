package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.FirebaseAuthDataSource;
import com.novabank.novapay.data.remote.firebase.FirestoreUserDataSource;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.repository.UserRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Singleton
public class UserRepositoryImpl implements UserRepository {

    private final FirebaseAuthDataSource authDataSource;
    private final FirestoreUserDataSource userDataSource;

    @Inject
    public UserRepositoryImpl(
            FirebaseAuthDataSource authDataSource,
            FirestoreUserDataSource userDataSource
    ) {
        this.authDataSource = authDataSource;
        this.userDataSource = userDataSource;
    }

    @Override
    public Observable<AppResult<User>> observeMyProfile() {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Observable.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để xem hồ sơ."));
        }
        return userDataSource.observeUser(uid);
    }

    @Override
    public Single<AppResult<User>> getMyProfile() {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Vui lòng đăng nhập để xem hồ sơ."));
        }
        return userDataSource.getUser(uid);
    }

    @Override
    public Single<AppResult<User>> getUserById(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Thiếu mã người dùng."));
        }
        return userDataSource.getUser(uid.trim());
    }

    @Override
    public Single<AppResult<Void>> updateMyProfile(String phone, String address, String avatarUrl) {
        String uid = authDataSource.getCurrentUid();
        if (uid == null || uid.isEmpty()) {
            return Single.just(AppResult.error("UNAUTHENTICATED", "Phiên đăng nhập đã hết hạn."));
        }
        return userDataSource.updateAllowedProfileFields(uid, phone, address, avatarUrl);
    }
}
