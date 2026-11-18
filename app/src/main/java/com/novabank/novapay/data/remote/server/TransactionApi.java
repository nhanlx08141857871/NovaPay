package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.ConfirmTransactionRequestDto;
import com.novabank.novapay.data.dto.ConfirmTransactionResponseDto;
import com.novabank.novapay.data.dto.DraftRequestDto;
import com.novabank.novapay.data.dto.HealthResponseDto;
import com.novabank.novapay.data.dto.InitiateTransactionRequestDto;
import com.novabank.novapay.data.dto.InitiateTransactionResponseDto;
import com.novabank.novapay.data.dto.ResendOtpResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Retrofit API điều phối giao dịch, OTP và kiểm tra sức khỏe Backend Server.
 */
public interface TransactionApi {

    @POST("transactions/initiate")
    Single<InitiateTransactionResponseDto> initiate(@Body InitiateTransactionRequestDto body);

    @POST("transactions/confirm")
    Single<ConfirmTransactionResponseDto> confirm(@Body ConfirmTransactionRequestDto body);

    @POST("transactions/resend-otp")
    Single<ResendOtpResponseDto> resendOtp(@Body DraftRequestDto body);

    @GET("health")
    Single<HealthResponseDto> health();
}
