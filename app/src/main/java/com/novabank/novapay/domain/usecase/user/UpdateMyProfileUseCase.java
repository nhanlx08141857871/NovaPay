package com.novabank.novapay.domain.usecase.user;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.repository.UserRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class UpdateMyProfileUseCase {

    private static final String PHONE_REGEX = "^(0|\\+84)[0-9]{9,10}$";
    private static final String AVATAR_URL_REGEX = "^https?://\\S+$";

    private final UserRepository userRepository;

    @Inject
    public UpdateMyProfileUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Single<AppResult<Void>> execute(String phone, String address, String avatarUrl) {
        String normalizedPhone = phone != null ? phone.trim().replaceAll("[\\s.-]", "") : "";
        String normalizedAddress = address != null ? address.trim() : "";
        String normalizedAvatar = avatarUrl != null ? avatarUrl.trim() : "";

        if (!normalizedPhone.matches(PHONE_REGEX)) {
            return Single.just(AppResult.error(
                    "INVALID_PHONE",
                    "Số điện thoại phải gồm 10–11 chữ số (bắt đầu bằng 0 hoặc +84)."
            ));
        }
        if (normalizedAddress.isEmpty() || normalizedAddress.length() > 500) {
            return Single.just(AppResult.error(
                    "INVALID_ADDRESS",
                    "Địa chỉ liên hệ phải từ 1 đến 500 ký tự."
            ));
        }
        if (normalizedAvatar.length() > 2048
                || (!normalizedAvatar.isEmpty() && !normalizedAvatar.matches(AVATAR_URL_REGEX))) {
            return Single.just(AppResult.error(
                    "INVALID_AVATAR",
                    "Liên kết ảnh đại diện phải bắt đầu bằng http:// hoặc https:// (tối đa 2048 ký tự)."
            ));
        }
        return userRepository.updateMyProfile(normalizedPhone, normalizedAddress, normalizedAvatar);
    }
}
