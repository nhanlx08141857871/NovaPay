package com.novabank.novapay.domain.model.utility;

import java.util.Objects;

public final class ServiceItem {

    private final String itemId;
    private final ServiceCategory category;
    private final String title;
    private final long price;
    private final String details;
    private final String imageUrl;

    public ServiceItem(
            String itemId,
            ServiceCategory category,
            String title,
            long price,
            String details,
            String imageUrl
    ) {
        this.itemId = Objects.requireNonNull(itemId, "itemId must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.title = title != null ? title : "";
        this.price = price;
        this.details = details != null ? details : "";
        this.imageUrl = imageUrl != null ? imageUrl : "";
    }

    public String getItemId() {
        return itemId;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public long getPrice() {
        return price;
    }

    public String getDetails() {
        return details;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
