package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.EkycSubmitRequestDto;
import com.novabank.novapay.data.dto.EkycSubmitResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface EkycApi {

    @POST("ekyc/submit")
    Single<EkycSubmitResponseDto> submitEkyc(@Body EkycSubmitRequestDto request);
}
