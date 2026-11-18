package com.novabank.novapay.presentation.customer.utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemUtilityBillBinding;
import com.novabank.novapay.domain.model.utility.UtilityBill;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách hóa đơn Điện / Nước chưa thanh toán (utility_bills).
 */
public class UtilityBillAdapter extends RecyclerView.Adapter<UtilityBillAdapter.ViewHolder> {

    public interface OnPayBillClickListener {
        void onPayBill(@NonNull UtilityBill bill);
    }

    private final List<UtilityBill> bills = new ArrayList<>();
    private final OnPayBillClickListener listener;

    public UtilityBillAdapter(@NonNull OnPayBillClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<UtilityBill> newBills) {
        bills.clear();
        bills.addAll(newBills);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUtilityBillBinding binding = ItemUtilityBillBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(bills.get(position));
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemUtilityBillBinding binding;

        ViewHolder(@NonNull ItemUtilityBillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull UtilityBill bill) {
            Context ctx = binding.getRoot().getContext();
            binding.tvBillProvider.setText(bill.getProviderName());
            binding.tvBillAmount.setText(CurrencyFormatter.formatVnd(bill.getAmount()));
            binding.tvBillCustomer.setText(ctx.getString(
                    R.string.money_bills_customer_format,
                    bill.getCustomerName(),
                    bill.getCustomerCode()
            ));
            String dueStr = bill.getDueDate() != null ? DateUtils.formatDate(bill.getDueDate()) : "-";
            binding.tvBillPeriodDue.setText(ctx.getString(
                    R.string.money_bills_period_format,
                    bill.getPeriod(),
                    dueStr
            ));
            binding.btnPayBill.setOnClickListener(v -> listener.onPayBill(bill));
        }
    }
}
