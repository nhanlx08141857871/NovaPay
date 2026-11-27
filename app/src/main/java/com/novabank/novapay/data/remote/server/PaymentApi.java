package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.DraftRequestDto;
import com.novabank.novapay.data.dto.FinalizePaymentResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PaymentApi {

    @POST("payments/finalize")
    Single<FinalizePaymentResponseDto> finalizePayment(@Body DraftRequestDto body);
}
