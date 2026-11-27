package com.novabank.novapay.data.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.data.dto.EkycSubmitRequestDto;
import com.novabank.novapay.data.dto.FaceCheckRequestDto;
import com.novabank.novapay.data.mapper.HttpErrorMapper;
import com.novabank.novapay.data.remote.server.EkycApi;
import com.novabank.novapay.data.remote.server.FaceCheckApi;
import com.novabank.novapay.domain.repository.EkycRepository;

import java.time.Instant;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public class EkycRepositoryImpl implements EkycRepository {

    private final EkycApi ekycApi;
    private final FaceCheckApi faceCheckApi;

    @Inject
    public EkycRepositoryImpl(EkycApi ekycApi, FaceCheckApi faceCheckApi) {
        this.ekycApi = ekycApi;
        this.faceCheckApi = faceCheckApi;
    }

    @Override
    public Single<AppResult<String>> submitEkyc(String imageBase64, String targetUid) {
        return ekycApi.submitEkyc(new EkycSubmitRequestDto(imageBase64, targetUid))
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    String status = response.getStatus() != null
                            ? response.getStatus()
                            : Constants.EKYC_STATUS_PENDING;
                    return AppResult.success(status);
                })
                .onErrorReturn(throwable -> HttpErrorMapper.toAppResultError(
                        throwable,
                        "Không thể gửi ảnh định danh eKYC. Vui lòng thử lại."
                ));
    }

    @Override
    public Single<AppResult<Long>> submitFaceCheck(String draftId, String imageBase64) {
        return faceCheckApi.submitFaceCheck(new FaceCheckRequestDto(draftId, imageBase64))
                .subscribeOn(Schedulers.io())
                .map(response -> {
                    if (!response.isOk()) {
                        return AppResult.<Long>error(
                                "FACE_MISMATCH",
                                "Khuôn mặt không khớp với hồ sơ eKYC đã đăng ký. Vui lòng thử lại!"
                        );
                    }
                    long expiresAtMs = parseEpochMillis(response.getOtpExpiresAt());
                    return AppResult.success(expiresAtMs);
                })
                .onErrorReturn(throwable -> HttpErrorMapper.toAppResultError(
                        throwable,
                        "Không thể xác minh khuôn mặt lúc này. Vui lòng thử lại."
                ));
    }

    private static long parseEpochMillis(String raw) {
        if (raw != null && !raw.trim().isEmpty()) {
            String trimmed = raw.trim();
            try {
                return Instant.parse(trimmed).toEpochMilli();
            } catch (Exception ignored) {
                try {
                    return Long.parseLong(trimmed);
                } catch (NumberFormatException ignoredAgain) {
                }
            }
        }
        return System.currentTimeMillis() + (Constants.OTP_EXPIRY_SECONDS * 1000L);
    }
}
