package com.novabank.novapay.domain.model.branch;

import java.util.Objects;

public final class Branch {

    private final String branchId;
    private final String name;
    private final String address;
    private final GeoLocation location;
    private final String phone;
    private final String openHours;
    private final BranchType type;

    public Branch(
            String branchId,
            String name,
            String address,
            GeoLocation location,
            String phone,
            String openHours,
            BranchType type
    ) {
        this.branchId = Objects.requireNonNull(branchId, "branchId must not be null");
        this.name = name != null ? name : "";
        this.address = address != null ? address : "";
        this.location = Objects.requireNonNull(location, "location must not be null");
        this.phone = phone != null ? phone : "";
        this.openHours = openHours != null ? openHours : "";
        this.type = type != null ? type : BranchType.BRANCH;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public GeoLocation getLocation() {
        return location;
    }

    public String getPhone() {
        return phone;
    }

    public String getOpenHours() {
        return openHours;
    }

    public BranchType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Branch that)) return false;
        return Objects.equals(branchId, that.branchId)
                && Objects.equals(name, that.name)
                && Objects.equals(address, that.address)
                && Objects.equals(location, that.location)
                && Objects.equals(phone, that.phone)
                && Objects.equals(openHours, that.openHours)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchId, name, address, location, phone, openHours, type);
    }
}
