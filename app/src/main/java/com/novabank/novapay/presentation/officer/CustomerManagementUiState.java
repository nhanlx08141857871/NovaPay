package com.novabank.novapay.presentation.officer;

import com.novabank.novapay.domain.model.user.CustomerProfile;

import java.util.List;

public final class CustomerManagementUiState {

    private final List<CustomerProfile> customers;
    private final String searchQuery;

    public CustomerManagementUiState(List<CustomerProfile> customers, String searchQuery) {
        this.customers = customers;
        this.searchQuery = searchQuery != null ? searchQuery : "";
    }

    public List<CustomerProfile> getCustomers() {
        return customers;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}
