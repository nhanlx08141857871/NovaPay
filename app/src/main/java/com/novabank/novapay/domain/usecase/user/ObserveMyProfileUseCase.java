package com.novabank.novapay.domain.usecase.user;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.repository.UserRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

/**
 * UseCase lắng nghe realtime hồ sơ cá nhân của người dùng hiện tại.
 */
public final class ObserveMyProfileUseCase {

    private final UserRepository userRepository;

    @Inject
    public ObserveMyProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Observable<AppResult<User>> execute() {
        return userRepository.observeMyProfile();
    }
}
