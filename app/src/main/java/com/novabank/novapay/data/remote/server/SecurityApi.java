package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.SecurityStatusResponseDto;
import com.novabank.novapay.data.dto.SetTransactionPinRequestDto;
import com.novabank.novapay.data.dto.SetTransactionPinResponseDto;
import com.novabank.novapay.data.dto.UpdateAccountControlsRequestDto;
import com.novabank.novapay.data.dto.UpdateAccountControlsResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SecurityApi {

    @GET("security/status")
    Single<SecurityStatusResponseDto> getSecurityStatus();

    @POST("security/pin/set")
    Single<SetTransactionPinResponseDto> setTransactionPin(@Body SetTransactionPinRequestDto body);

    @PATCH("security/accounts/{accountId}/controls")
    Single<UpdateAccountControlsResponseDto> updateAccountControls(
            @Path("accountId") String accountId,
            @Body UpdateAccountControlsRequestDto body
    );
}
