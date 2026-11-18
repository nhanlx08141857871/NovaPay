package com.novabank.novapay.presentation.customer.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.novabank.novapay.core.util.DateUtils;
import com.novabank.novapay.databinding.ItemChatBotBinding;
import com.novabank.novapay.databinding.ItemChatUserBinding;
import com.novabank.novapay.domain.model.chat.ChatAction;
import com.novabank.novapay.domain.model.chat.ChatMessage;
import com.novabank.novapay.domain.model.chat.ChatRole;

import java.util.List;
import java.util.Objects;

public final class ChatMessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    public interface OnActionClickListener {
        void onActionClick(@NonNull ChatAction action);
    }

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ChatMessage oldItem,
                        @NonNull ChatMessage newItem
                ) {
                    return Objects.equals(oldItem.getMessageId(), newItem.getMessageId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ChatMessage oldItem,
                        @NonNull ChatMessage newItem
                ) {
                    return oldItem.equals(newItem);
                }
            };

    private final OnActionClickListener actionClickListener;

    public ChatMessageAdapter(@NonNull OnActionClickListener actionClickListener) {
        super(DIFF_CALLBACK);
        this.actionClickListener = actionClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getRole() == ChatRole.USER ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            return new UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false));
        }
        return new BotViewHolder(ItemChatBotBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = getItem(position);
        if (holder instanceof UserViewHolder userHolder) {
            userHolder.bind(msg);
        } else if (holder instanceof BotViewHolder botHolder) {
            botHolder.bind(msg, actionClickListener);
        }
    }

    static final class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatUserBinding binding;

        UserViewHolder(ItemChatUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message) {
            binding.tvUserMessageText.setText(message.getText());
            binding.tvUserMessageTime.setText(DateUtils.formatDateTime(message.getCreatedAt()));
        }
    }

    static final class BotViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatBotBinding binding;

        BotViewHolder(ItemChatBotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatMessage message, OnActionClickListener listener) {
            binding.tvBotMessageText.setText(message.getText());
            binding.tvBotMessageTime.setText(DateUtils.formatDateTime(message.getCreatedAt()));

            binding.chipGroupActions.removeAllViews();
            List<ChatAction> actions = message.getActions();
            if (actions == null || actions.isEmpty()) {
                binding.chipGroupActions.setVisibility(View.GONE);
            } else {
                binding.chipGroupActions.setVisibility(View.VISIBLE);
                for (ChatAction action : actions) {
                    Chip chip = new Chip(binding.chipGroupActions.getContext());
                    chip.setText(action.getLabel());
                    chip.setEnsureMinTouchTargetSize(true);
                    chip.setClickable(true);
                    chip.setOnClickListener(v -> listener.onActionClick(action));
                    binding.chipGroupActions.addView(chip);
                }
            }
        }
    }
}
