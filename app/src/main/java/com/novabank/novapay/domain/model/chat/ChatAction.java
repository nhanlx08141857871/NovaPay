package com.novabank.novapay.domain.model.chat;

import com.novabank.novapay.core.common.Constants;

import java.util.Objects;

public final class ChatAction {

    private final String label;
    private final String deepLink;

    public ChatAction(String label, String deepLink) {
        this.label = label != null ? label : "";
        this.deepLink = deepLink != null ? deepLink : "";
    }

    public String getLabel() {
        return label;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public boolean hasValidDeepLink() {
        return deepLink.startsWith(Constants.DEEPLINK_SCHEME + "://") && !label.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatAction that)) return false;
        return Objects.equals(label, that.label) && Objects.equals(deepLink, that.deepLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, deepLink);
    }
}
