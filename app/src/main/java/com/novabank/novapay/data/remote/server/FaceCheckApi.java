package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.FaceCheckRequestDto;
import com.novabank.novapay.data.dto.FaceCheckResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface FaceCheckApi {

    @POST("transactions/face-check")
    Single<FaceCheckResponseDto> submitFaceCheck(@Body FaceCheckRequestDto request);
}
