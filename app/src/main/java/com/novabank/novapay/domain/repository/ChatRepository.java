package com.novabank.novapay.domain.repository;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.chat.ChatMessage;
import com.novabank.novapay.domain.model.chat.ChatReply;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public interface ChatRepository {

    Observable<AppResult<List<ChatMessage>>> observeChatMessages();

    Single<AppResult<ChatReply>> sendChatMessage(String message);
}
