package com.novabank.novapay.domain.usecase.chat;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.chat.ChatReply;
import com.novabank.novapay.domain.repository.ChatRepository;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class SendChatMessageUseCase {

    private final ChatRepository chatRepository;

    @Inject
    public SendChatMessageUseCase(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public Single<AppResult<ChatReply>> execute(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Single.just(AppResult.error("INVALID_ARGUMENT", "Vui lòng nhập nội dung câu hỏi."));
        }
        return chatRepository.sendChatMessage(message.trim());
    }
}
