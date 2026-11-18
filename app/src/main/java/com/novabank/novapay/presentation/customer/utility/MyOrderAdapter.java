package com.novabank.novapay.presentation.customer.utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novabank.novapay.R;
import com.novabank.novapay.core.util.CurrencyFormatter;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemServiceOrderBinding;
import com.novabank.novapay.domain.model.utility.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách đơn hàng / vé dịch vụ của khách hàng (orders).
 */
public class MyOrderAdapter extends RecyclerView.Adapter<MyOrderAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(@NonNull Order order);
    }

    private final List<Order> orders = new ArrayList<>();
    private final OnOrderClickListener listener;

    public MyOrderAdapter(@NonNull OnOrderClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Order> newOrders) {
        orders.clear();
        orders.addAll(newOrders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemServiceOrderBinding binding = ItemServiceOrderBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemServiceOrderBinding binding;

        ViewHolder(@NonNull ItemServiceOrderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Order order) {
            Context ctx = binding.getRoot().getContext();
            binding.tvOrderItemTitle.setText(order.getCategory().name() + " • " + order.getItemId());
            binding.tvOrderTotalAmount.setText(CurrencyFormatter.formatVnd(order.getTotalAmount()));
            binding.tvOrderCodeAndQty.setText(ctx.getString(
                    R.string.money_services_order_code_format,
                    order.getOrderId(),
                    order.getQuantity()
            ));
            String dateStr = order.getCreatedAt() != null
                    ? DateUtils.formatDateTime(order.getCreatedAt())
                    : "-";
            binding.tvOrderStatusAndDate.setText(ctx.getString(
                    R.string.money_services_order_status_format,
                    order.getStatus(),
                    dateStr
            ));
            binding.getRoot().setOnClickListener(v -> listener.onOrderClick(order));
        }
    }
}
