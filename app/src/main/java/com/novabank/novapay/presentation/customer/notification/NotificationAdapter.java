package com.novabank.novapay.presentation.customer.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemNotificationBinding;
import com.novabank.novapay.domain.model.notification.AppNotification;
import com.novabank.novapay.domain.model.notification.NotificationCategory;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(@NonNull AppNotification notification);
    }

    private final List<AppNotification> items = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(@NonNull OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<AppNotification> notifications) {
        items.clear();
        items.addAll(notifications);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotificationBinding binding;

        NotificationViewHolder(@NonNull ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull AppNotification item, @NonNull OnNotificationClickListener listener) {
            Context context = binding.getRoot().getContext();
            binding.tvNotifTitle.setText(item.getTitle());
            binding.tvNotifBody.setText(item.getBody());
            binding.tvNotifTime.setText(item.getCreatedAt() != null
                    ? DateUtils.formatDateTime(item.getCreatedAt())
                    : "-");

            if (item.getAccountNumber() != null && !item.getAccountNumber().isEmpty()) {
                binding.tvNotifAccount.setVisibility(View.VISIBLE);
                binding.tvNotifAccount.setText(context.getString(
                        R.string.core_notif_account_format,
                        item.getAccountNumber()
                ));
            } else {
                binding.tvNotifAccount.setVisibility(View.GONE);
            }

            if (item.getAmount() != null && item.getAmount() > 0L) {
                binding.layoutAmountRow.setVisibility(View.VISIBLE);
                boolean hasDirection = item.getDirection() != null;
                boolean isIn = item.isInflow();
                binding.tvNotifAmount.setText(hasDirection
                        ? CurrencyFormatter.formatSignedVnd(item.getAmount(), isIn)
                        : CurrencyFormatter.formatVnd(item.getAmount()));
                int amountColor = (!hasDirection || isIn)
                        ? R.color.novapay_income_positive
                        : R.color.novapay_expense_negative;
                binding.tvNotifAmount.setTextColor(ContextCompat.getColor(context, amountColor));

                if (item.getBalanceAfter() != null) {
                    binding.tvNotifBalanceAfter.setVisibility(View.VISIBLE);
                    binding.tvNotifBalanceAfter.setText(context.getString(
                            R.string.core_notif_balance_after,
                            CurrencyFormatter.formatVnd(item.getBalanceAfter())
                    ));
                } else {
                    binding.tvNotifBalanceAfter.setVisibility(View.GONE);
                }
            } else {
                binding.layoutAmountRow.setVisibility(View.GONE);
            }

            boolean unread = !item.isRead();
            binding.viewUnreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
            binding.cardNotification.setStrokeColor(ContextCompat.getColor(
                    context,
                    unread ? R.color.np_badge_verified_stroke : R.color.np_border_hairline
            ));
            binding.cardNotification.setCardBackgroundColor(unread
                    ? ContextCompat.getColor(context, R.color.np_accent_emerald_soft)
                    : com.google.android.material.color.MaterialColors.getColor(
                            binding.getRoot(),
                            com.google.android.material.R.attr.colorSurface
                    ));

            bindCategoryIcon(context, item.getCategory());
            binding.getRoot().setOnClickListener(v -> listener.onNotificationClick(item));
        }

        private void bindCategoryIcon(@NonNull Context context, @NonNull NotificationCategory category) {
            if (category == NotificationCategory.SAVING) {
                binding.frameCategoryIcon.setBackgroundResource(R.drawable.bg_np_circle_soft_emerald);
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_np_trending_rate);
                binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.np_accent_emerald));
            } else if (category == NotificationCategory.SECURITY) {
                binding.frameCategoryIcon.setBackgroundResource(R.drawable.bg_np_circle_soft_danger);
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_np_shield_check);
                binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.novapay_expense_negative));
            } else if (category == NotificationCategory.EKYC) {
                binding.frameCategoryIcon.setBackgroundResource(R.drawable.bg_np_circle_soft_neutral);
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_np_ekyc_badge);
                binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.np_brand_navy));
            } else {
                binding.frameCategoryIcon.setBackgroundResource(R.drawable.bg_np_circle_soft_emerald);
                binding.ivCategoryIcon.setImageResource(R.drawable.ic_np_bell);
                binding.ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.np_accent_emerald));
            }
        }
    }
}
