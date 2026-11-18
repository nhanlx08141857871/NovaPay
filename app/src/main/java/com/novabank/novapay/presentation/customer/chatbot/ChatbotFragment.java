package com.novabank.novapay.presentation.customer.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.core.ui.BaseFragment;
import com.novabank.novapay.core.ui.StateLayoutHelper;
import com.novabank.novapay.databinding.FragmentChatbotBinding;
import com.novabank.novapay.domain.model.chat.ChatAction;
import com.novabank.novapay.domain.model.chat.ChatMessage;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatbotFragment extends BaseFragment<FragmentChatbotBinding> {

    private ChatbotViewModel viewModel;
    private StateLayoutHelper stateLayoutHelper;
    private ChatMessageAdapter adapter;

    @NonNull
    @Override
    protected FragmentChatbotBinding inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    ) {
        return FragmentChatbotBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        viewModel = new ViewModelProvider(this).get(ChatbotViewModel.class);
        FragmentChatbotBinding b = getBinding();

        stateLayoutHelper = new StateLayoutHelper(
                b.contentContainer,
                b.stateLoading,
                b.stateEmpty,
                b.stateError,
                () -> viewModel.loadMessages()
        );
        b.stateEmpty.tvEmptyTitle.setText(R.string.chatbot_empty_title);
        b.stateEmpty.tvEmptySubtitle.setText(R.string.chatbot_empty_subtitle);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        b.contentContainer.setLayoutManager(layoutManager);

        adapter = new ChatMessageAdapter(this::handleChatActionClick);
        b.contentContainer.setAdapter(adapter);

        b.toolbarChatbot.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack()
        );

        b.btnSendChat.setOnClickListener(v -> {
            String text = b.etChatInput.getText() != null ? b.etChatInput.getText().toString() : "";
            viewModel.sendMessage(text);
        });

        b.chipSuggestBalance.setOnClickListener(v ->
                viewModel.sendMessage(getString(R.string.chatbot_suggestion_balance))
        );
        b.chipSuggestBills.setOnClickListener(v ->
                viewModel.sendMessage(getString(R.string.chatbot_suggestion_bills))
        );
        b.chipSuggestBranches.setOnClickListener(v ->
                viewModel.sendMessage(getString(R.string.chatbot_suggestion_branches))
        );
    }

    @Override
    protected void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || !isAdded() || getView() == null) {
                return;
            }
            stateLayoutHelper.render(state);
            if (state.getStatus() == UiState.Status.SUCCESS && state.getData() != null) {
                List<ChatMessage> messages = state.getData().getMessages();
                adapter.submitList(messages, () -> {
                    if (isAdded() && getView() != null && !messages.isEmpty()) {
                        getBinding().contentContainer.scrollToPosition(messages.size() - 1);
                    }
                });
            }
        });

        viewModel.getSendingMessage().observe(getViewLifecycleOwner(), sending -> {
            if (isAdded() && getView() != null) {
                boolean busy = Boolean.TRUE.equals(sending);
                FragmentChatbotBinding b = getBinding();
                b.progressSendingChat.setVisibility(busy ? View.VISIBLE : View.GONE);
                b.btnSendChat.setEnabled(!busy);
            }
        });

        viewModel.getMessageSentClearInputEvent().observe(getViewLifecycleOwner(), clear -> {
            if (Boolean.TRUE.equals(clear) && isAdded() && getView() != null) {
                getBinding().etChatInput.setText("");
            }
        });

        viewModel.getErrorMessageEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                showSnackbar(msg);
            }
        });

        viewModel.getErrorResEvent().observe(getViewLifecycleOwner(), resId -> {
            if (resId != null) {
                showSnackbar(getString(resId));
            }
        });
    }

    private void handleChatActionClick(@NonNull ChatAction action) {
        if (!action.hasValidDeepLink()) {
            showSnackbar(getString(R.string.chatbot_deeplink_unavailable, action.getDeepLink()));
            return;
        }
        try {
            navigateDeepLink(action.getDeepLink());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showSnackbar(getString(R.string.chatbot_deeplink_unavailable, action.getDeepLink()));
        }
    }
}
