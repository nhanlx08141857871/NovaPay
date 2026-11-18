package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.AccountLookupResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit API tra cứu tài khoản nội bộ an toàn (GET /accounts/lookup).
 */
public interface AccountLookupApi {

    @GET("accounts/lookup")
    Single<AccountLookupResponseDto> lookupAccount(@Query("accountNumber") String accountNumber);
}
