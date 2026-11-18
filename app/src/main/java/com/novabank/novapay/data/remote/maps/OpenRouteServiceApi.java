package com.novabank.novapay.data.remote.maps;

import com.novabank.novapay.data.dto.OpenRouteDirectionsRequestDto;
import com.novabank.novapay.data.dto.OpenRouteDirectionsResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenRouteServiceApi {

    @POST("v2/directions/foot-walking/geojson")
    Single<OpenRouteDirectionsResponseDto> getWalkingDirections(
            @Header("Authorization") String apiKey,
            @Body OpenRouteDirectionsRequestDto body
    );
}
