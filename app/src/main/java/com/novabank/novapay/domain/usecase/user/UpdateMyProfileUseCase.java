package com.novabank.novapay.domain.usecase.user;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.UserRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

/**
 * UseCase cập nhật an toàn các trường được phép (phone, address, avatarUrl) theo firestore.rules.
 */
public final class UpdateMyProfileUseCase {

    private final UserRepository userRepository;

    @Inject
    public UpdateMyProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Single<AppResult<Void>> execute(String phone, String address, String avatarUrl) {
        String normalizedPhone = phone != null ? phone.trim() : "";
        String normalizedAddress = address != null ? address.trim() : "";
        String normalizedAvatar = avatarUrl != null ? avatarUrl.trim() : "";

        if (normalizedPhone.length() < 9 || normalizedPhone.length() > 15) {
            return Single.just(AppResult.error(
                    "INVALID_PHONE",
                    "Số điện thoại không hợp lệ (cần từ 9 đến 15 ký tự)."
            ));
        }
        if (normalizedAddress.isEmpty()) {
            return Single.just(AppResult.error(
                    "INVALID_ADDRESS",
                    "Địa chỉ liên hệ không được để trống."
            ));
        }
        return userRepository.updateMyProfile(normalizedPhone, normalizedAddress, normalizedAvatar);
    }
}
