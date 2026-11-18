package com.novabank.novapay.domain.model.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatReply {

    private final String reply;
    private final List<ChatAction> actions;

    public ChatReply(String reply, List<ChatAction> actions) {
        this.reply = reply != null ? reply : "";
        this.actions = actions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(actions));
    }

    public String getReply() {
        return reply;
    }

    public List<ChatAction> getActions() {
        return actions;
    }
}
