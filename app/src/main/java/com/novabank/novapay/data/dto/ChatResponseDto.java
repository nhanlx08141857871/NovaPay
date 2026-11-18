package com.novabank.novapay.data.dto;

import java.util.List;

public final class ChatResponseDto {
    private String reply;
    private List<ChatActionDto> actions;

    public String getReply() {
        return reply;
    }

    public List<ChatActionDto> getActions() {
        return actions;
    }
}
