package com.novabank.novapay.presentation.officer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.databinding.ItemCustomerBinding;
import com.novabank.novapay.domain.model.user.CustomerProfile;

import java.util.Objects;

public final class CustomerAdapter extends ListAdapter<CustomerProfile, CustomerAdapter.ViewHolder> {

    public interface OnCustomerActionListener {
        void onEditCustomer(@NonNull CustomerProfile customer);
        void onToggleLock(@NonNull CustomerProfile customer);
    }

    private static final DiffUtil.ItemCallback<CustomerProfile> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull CustomerProfile oldItem, @NonNull CustomerProfile newItem) {
                    return Objects.equals(oldItem.getUid(), newItem.getUid());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CustomerProfile oldItem, @NonNull CustomerProfile newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnCustomerActionListener listener;

    public CustomerAdapter(@NonNull OnCustomerActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCustomerBinding binding = ItemCustomerBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCustomerBinding binding;

        ViewHolder(ItemCustomerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CustomerProfile item, OnCustomerActionListener listener) {
            Context ctx = binding.getRoot().getContext();
            binding.tvCustomerName.setText(item.getFullName());
            binding.tvCustomerEmail.setText(item.getEmail());
            binding.tvCustomerPhone.setText(ctx.getString(R.string.officer_customer_phone_format, item.getPhone()));
            binding.tvCustomerNationalId.setText(
                    ctx.getString(R.string.officer_customer_cccd_format, item.getNationalId())
            );

            if (item.isLocked()) {
                binding.tvCustomerStatusBadge.setText(R.string.officer_customer_status_locked);
                binding.tvCustomerStatusBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_expense_negative)
                );
                binding.btnToggleLock.setText(R.string.officer_customer_unlock_action);
            } else {
                binding.tvCustomerStatusBadge.setText(R.string.officer_customer_status_active);
                binding.tvCustomerStatusBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_income_positive)
                );
                binding.btnToggleLock.setText(R.string.officer_customer_lock_action);
            }

            String ekycStatus = item.getEkycStatus();
            if (Constants.EKYC_STATUS_VERIFIED.equalsIgnoreCase(ekycStatus)) {
                binding.tvCustomerEkycBadge.setText(R.string.officer_customer_ekyc_verified);
                binding.tvCustomerEkycBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_verified_badge)
                );
            } else if (Constants.EKYC_STATUS_PENDING.equalsIgnoreCase(ekycStatus)) {
                binding.tvCustomerEkycBadge.setText(R.string.officer_customer_ekyc_pending);
                binding.tvCustomerEkycBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_warning_pending)
                );
            } else if (Constants.EKYC_STATUS_REJECTED.equalsIgnoreCase(ekycStatus)) {
                binding.tvCustomerEkycBadge.setText(R.string.officer_customer_ekyc_rejected);
                binding.tvCustomerEkycBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_expense_negative)
                );
            } else {
                binding.tvCustomerEkycBadge.setText(R.string.officer_customer_ekyc_none);
                binding.tvCustomerEkycBadge.setTextColor(
                        ContextCompat.getColor(ctx, R.color.novapay_warning_pending)
                );
            }

            binding.btnEditCustomer.setOnClickListener(v -> listener.onEditCustomer(item));
            binding.btnToggleLock.setOnClickListener(v -> listener.onToggleLock(item));
        }
    }
}
