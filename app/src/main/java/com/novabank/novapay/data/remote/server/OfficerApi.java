package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.CreateCustomerRequestDto;
import com.novabank.novapay.data.dto.CreateCustomerResponseDto;
import com.novabank.novapay.data.dto.EkycReviewRequestDto;
import com.novabank.novapay.data.dto.EkycReviewResponseDto;
import com.novabank.novapay.data.dto.OkResponseDto;
import com.novabank.novapay.data.dto.SaveInterestRateRequestDto;
import com.novabank.novapay.data.dto.SaveInterestRateResponseDto;
import com.novabank.novapay.data.dto.UpdateCustomerRequestDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OfficerApi {

    @POST("officer/customers")
    Single<CreateCustomerResponseDto> createCustomer(@Body CreateCustomerRequestDto body);

    @PATCH("officer/customers/{uid}")
    Single<OkResponseDto> updateCustomer(
            @Path("uid") String uid,
            @Body UpdateCustomerRequestDto body
    );

    @POST("officer/interest-rates")
    Single<SaveInterestRateResponseDto> saveInterestRate(@Body SaveInterestRateRequestDto body);

    @POST("ekyc/review")
    Single<EkycReviewResponseDto> reviewEkyc(@Body EkycReviewRequestDto body);
}
