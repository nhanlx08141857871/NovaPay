package com.novabank.novapay.presentation.customer.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.databinding.ItemAccountCardBinding;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.account.AccountStatus;
import com.novabank.novapay.domain.model.account.AccountType;

import java.util.ArrayList;
import java.util.List;

public class AccountCardAdapter extends RecyclerView.Adapter<AccountCardAdapter.AccountViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(@NonNull Account account);
    }

    private final List<Account> items = new ArrayList<>();
    private final OnAccountClickListener listener;
    private boolean balanceHidden = false;

    public AccountCardAdapter(@NonNull OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Account> accounts, boolean hidden) {
        items.clear();
        items.addAll(accounts);
        this.balanceHidden = hidden;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAccountCardBinding binding = ItemAccountCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AccountViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        holder.bind(items.get(position), balanceHidden, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class AccountViewHolder extends RecyclerView.ViewHolder {

        private final ItemAccountCardBinding binding;

        AccountViewHolder(@NonNull ItemAccountCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                @NonNull Account account,
                boolean balanceHidden,
                @NonNull OnAccountClickListener listener
        ) {
            Context context = binding.getRoot().getContext();
            binding.tvAccountType.setText(resolveTypeLabel(context, account.getType()));
            binding.viewAccountAccentBar.setBackgroundColor(resolveTypeAccentColor(context, account.getType()));
            binding.tvAccountStatus.setText(resolveStatusLabel(context, account.getStatus()));
            binding.tvAccountStatus.setTextColor(resolveStatusColor(context, account.getStatus()));
            binding.tvAccountStatus.setBackgroundResource(resolveStatusBadgeBg(account.getStatus()));
            binding.tvAccountNumber.setText(account.getAccountNumber());

            if (account.getType() == AccountType.MORTGAGE && account.getMortgage() != null) {
                binding.tvAccountBalanceLabel.setText(R.string.core_account_outstanding_label);
                long outstanding = account.getMortgage().getOutstandingBalance();
                binding.tvAccountBalance.setText(
                        balanceHidden
                                ? context.getString(R.string.core_home_masked_balance)
                                : CurrencyFormatter.formatVnd(outstanding)
                );
            } else {
                binding.tvAccountBalanceLabel.setText(R.string.core_account_balance_label);
                binding.tvAccountBalance.setText(
                        balanceHidden
                                ? context.getString(R.string.core_home_masked_balance)
                                : CurrencyFormatter.formatVnd(account.getBalance())
                );
            }

            binding.getRoot().setOnClickListener(v -> listener.onAccountClick(account));
        }

        private static String resolveTypeLabel(Context context, AccountType type) {
            if (type == AccountType.SAVING) {
                return context.getString(R.string.core_account_type_saving);
            }
            if (type == AccountType.MORTGAGE) {
                return context.getString(R.string.core_account_type_mortgage);
            }
            return context.getString(R.string.core_account_type_checking);
        }

        private static int resolveTypeAccentColor(Context context, AccountType type) {
            if (type == AccountType.SAVING) {
                return ContextCompat.getColor(context, R.color.np_accent_emerald);
            }
            if (type == AccountType.MORTGAGE) {
                return ContextCompat.getColor(context, R.color.novapay_warning_pending);
            }
            return ContextCompat.getColor(context, R.color.np_brand_navy);
        }

        private static String resolveStatusLabel(Context context, AccountStatus status) {
            if (status == AccountStatus.FROZEN) {
                return context.getString(R.string.core_account_status_frozen);
            }
            if (status == AccountStatus.CLOSED) {
                return context.getString(R.string.core_account_status_closed);
            }
            return context.getString(R.string.core_account_status_active);
        }

        private static int resolveStatusColor(Context context, AccountStatus status) {
            if (status == AccountStatus.FROZEN) {
                return ContextCompat.getColor(context, R.color.novapay_warning_pending);
            }
            if (status == AccountStatus.CLOSED) {
                return ContextCompat.getColor(context, R.color.novapay_expense_negative);
            }
            return ContextCompat.getColor(context, R.color.novapay_income_positive);
        }

        private static int resolveStatusBadgeBg(AccountStatus status) {
            if (status == AccountStatus.FROZEN) {
                return R.drawable.bg_np_badge_pending;
            }
            if (status == AccountStatus.CLOSED) {
                return R.drawable.bg_np_badge_locked;
            }
            return R.drawable.bg_np_badge_verified;
        }
    }
}
