package com.novabank.novapay.presentation.officer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemAuditLogBinding;
import com.novabank.novapay.domain.model.audit.AuditLog;

import java.util.Objects;

public final class AuditLogAdapter extends ListAdapter<AuditLog, AuditLogAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<AuditLog> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull AuditLog oldItem, @NonNull AuditLog newItem) {
                    return Objects.equals(oldItem.getLogId(), newItem.getLogId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AuditLog oldItem, @NonNull AuditLog newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public AuditLogAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAuditLogBinding binding = ItemAuditLogBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAuditLogBinding binding;

        ViewHolder(ItemAuditLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AuditLog item) {
            binding.tvAuditAction.setText(item.getAction());
            binding.tvAuditTimestamp.setText(DateUtils.formatDateTime(item.getCreatedAt()));
            binding.tvAuditTarget.setText(binding.getRoot().getContext().getString(
                    R.string.officer_audit_item_target,
                    item.getTargetId()
            ));
            binding.tvAuditDetails.setText(item.getDetails());
            binding.tvAuditActor.setText(binding.getRoot().getContext().getString(
                    R.string.officer_audit_item_actor,
                    item.getActorUid()
            ));
        }
    }
}
