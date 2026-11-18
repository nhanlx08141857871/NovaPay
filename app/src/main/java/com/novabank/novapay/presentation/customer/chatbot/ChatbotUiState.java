package com.novabank.novapay.presentation.customer.chatbot;

import com.novabank.novapay.domain.model.chat.ChatMessage;

import java.util.List;

public final class ChatbotUiState {

    private final List<ChatMessage> messages;

    public ChatbotUiState(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }
}
