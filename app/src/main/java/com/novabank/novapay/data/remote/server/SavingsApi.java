package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.OpenSavingRequestDto;
import com.novabank.novapay.data.dto.OpenSavingResponseDto;
import com.novabank.novapay.data.dto.SettleSavingRequestDto;
import com.novabank.novapay.data.dto.SettleSavingResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SavingsApi {

    @POST("savings/open")
    Single<OpenSavingResponseDto> openSaving(@Body OpenSavingRequestDto body);

    @POST("savings/{accountId}/settle")
    Single<SettleSavingResponseDto> settleSaving(
            @Path("accountId") String accountId,
            @Body SettleSavingRequestDto body
    );
}
