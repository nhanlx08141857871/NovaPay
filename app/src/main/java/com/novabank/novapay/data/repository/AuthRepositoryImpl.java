package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.remote.firebase.FirebaseAuthDataSource;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.repository.AuthRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

@Singleton
public class AuthRepositoryImpl implements AuthRepository {

    private final FirebaseAuthDataSource authDataSource;

    @Inject
    public AuthRepositoryImpl(FirebaseAuthDataSource authDataSource) {
        this.authDataSource = authDataSource;
    }

    @Override
    public Single<AppResult<UserRole>> signIn(String email, String password) {
        return authDataSource.signIn(email, password);
    }

    @Override
    public Single<AppResult<UserRole>> refreshRole() {
        return authDataSource.refreshRole();
    }

    @Override
    public void signOut() {
        authDataSource.signOut();
    }

    @Override
    public boolean isSignedIn() {
        return authDataSource.isSignedIn();
    }

    @Override
    public String getCurrentUid() {
        return authDataSource.getCurrentUid();
    }

    @Override
    public Single<AppResult<Boolean>> changePassword(String currentPassword, String newPassword) {
        return authDataSource.changePassword(currentPassword, newPassword);
    }

    @Override
    public Single<AppResult<Boolean>> sendPasswordResetEmail(String email) {
        return authDataSource.sendPasswordResetEmail(email);
    }
}
