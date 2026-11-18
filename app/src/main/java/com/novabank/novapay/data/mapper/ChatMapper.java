package com.novabank.novapay.data.mapper;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.novabank.novapay.data.dto.ChatActionDto;
import com.novabank.novapay.data.dto.ChatResponseDto;
import com.novabank.novapay.domain.model.chat.ChatAction;
import com.novabank.novapay.domain.model.chat.ChatMessage;
import com.novabank.novapay.domain.model.chat.ChatReply;
import com.novabank.novapay.domain.model.chat.ChatRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ChatMapper {

    private ChatMapper() {
    }

    @Nullable
    public static ChatMessage toDomainMessage(@Nullable DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        String rawRole = doc.getString("role");
        ChatRole role = "USER".equalsIgnoreCase(rawRole) ? ChatRole.USER : ChatRole.BOT;
        String text = doc.getString("text");

        List<ChatAction> actions = new ArrayList<>();
        Object rawActions = doc.get("actions");
        if (rawActions instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object labelObj = map.get("label");
                    Object deepLinkObj = map.get("deepLink");
                    if (labelObj instanceof String label && deepLinkObj instanceof String deepLink) {
                        ChatAction action = new ChatAction(label, deepLink);
                        if (action.hasValidDeepLink()) {
                            actions.add(action);
                        }
                    }
                }
            }
        }

        return new ChatMessage(
                doc.getId(),
                role,
                text != null ? text : "",
                actions,
                FirestoreTimestampMapper.toInstant(doc.getTimestamp("createdAt"))
        );
    }

    public static ChatReply toDomainReply(@Nullable ChatResponseDto dto) {
        if (dto == null) {
            return new ChatReply("", Collections.emptyList());
        }
        List<ChatAction> actions = new ArrayList<>();
        if (dto.getActions() != null) {
            for (ChatActionDto actionDto : dto.getActions()) {
                if (actionDto != null) {
                    ChatAction action = new ChatAction(actionDto.getLabel(), actionDto.getDeepLink());
                    if (action.hasValidDeepLink()) {
                        actions.add(action);
                    }
                }
            }
        }
        return new ChatReply(dto.getReply(), actions);
    }
}
