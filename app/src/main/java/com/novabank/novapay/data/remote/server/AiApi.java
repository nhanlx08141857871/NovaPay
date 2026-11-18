package com.novabank.novapay.data.remote.server;

import com.novabank.novapay.data.dto.ChatRequestDto;
import com.novabank.novapay.data.dto.ChatResponseDto;

import io.reactivex.rxjava3.core.Single;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiApi {

    @POST("ai/chat")
    Single<ChatResponseDto> sendChatMessage(@Body ChatRequestDto body);
}
