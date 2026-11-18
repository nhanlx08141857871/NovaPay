package com.novabank.novapay.domain.model.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ChatMessage {

    private final String messageId;
    private final ChatRole role;
    private final String text;
    private final List<ChatAction> actions;
    private final Instant createdAt;

    public ChatMessage(
            String messageId,
            ChatRole role,
            String text,
            List<ChatAction> actions,
            Instant createdAt
    ) {
        this.messageId = Objects.requireNonNull(messageId, "messageId must not be null");
        this.role = role != null ? role : ChatRole.BOT;
        this.text = text != null ? text : "";
        this.actions = actions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(actions));
        this.createdAt = createdAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public ChatRole getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public List<ChatAction> getActions() {
        return actions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatMessage that)) return false;
        return Objects.equals(messageId, that.messageId)
                && role == that.role
                && Objects.equals(text, that.text)
                && Objects.equals(actions, that.actions)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, role, text, actions, createdAt);
    }
}
