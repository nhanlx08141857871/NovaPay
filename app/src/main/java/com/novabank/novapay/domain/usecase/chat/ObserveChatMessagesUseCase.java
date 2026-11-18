package com.novabank.novapay.domain.usecase.chat;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.chat.ChatMessage;
import com.novabank.novapay.domain.repository.ChatRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Observable;

public final class ObserveChatMessagesUseCase {

    private final ChatRepository chatRepository;

    @Inject
    public ObserveChatMessagesUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public Observable<AppResult<List<ChatMessage>>> execute() {
        return chatRepository.observeChatMessages();
    }
}
