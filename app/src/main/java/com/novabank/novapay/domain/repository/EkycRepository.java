package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;

import io.reactivex.rxjava3.core.Single;

public interface EkycRepository {

    Single<AppResult<String>> submitEkyc(String imageBase64, String targetUid);

    Single<AppResult<Long>> submitFaceCheck(String draftId, String imageBase64);
}
