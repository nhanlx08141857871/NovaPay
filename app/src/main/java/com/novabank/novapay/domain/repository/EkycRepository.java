package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;

import io.reactivex.rxjava3.core.Single;

/**
 * Hợp đồng gửi ảnh định danh eKYC và đối chiếu khuôn mặt giao dịch > 5.000.000 VNĐ (Pure Java 17).
 */
public interface EkycRepository {

    Single<AppResult<String>> submitEkyc(String imageBase64, String targetUid);

    Single<AppResult<Long>> submitFaceCheck(String draftId, String imageBase64);
}
