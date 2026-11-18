package com.novabank.novapay.presentation.customer.chatbot;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.R;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.chat.ChatMessage;
import com.novabank.novapay.domain.usecase.chat.ObserveChatMessagesUseCase;
import com.novabank.novapay.domain.usecase.chat.SendChatMessageUseCase;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

@HiltViewModel
public class ChatbotViewModel extends ViewModel {

    private final ObserveChatMessagesUseCase observeChatMessagesUseCase;
    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable observeDisposable;

    private final MutableLiveData<UiState<ChatbotUiState>> uiState =
            new MutableLiveData<>(UiState.loading());
    private final MutableLiveData<Boolean> sendingMessage = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorMessageEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> errorResEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> messageSentClearInputEvent = new SingleLiveEvent<>();

    @Inject
    public ChatbotViewModel(
            ObserveChatMessagesUseCase observeChatMessagesUseCase,
            SendChatMessageUseCase sendChatMessageUseCase
    ) {
        this.observeChatMessagesUseCase = observeChatMessagesUseCase;
        this.sendChatMessageUseCase = sendChatMessageUseCase;
        loadMessages();
    }

    public LiveData<UiState<ChatbotUiState>> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getSendingMessage() {
        return sendingMessage;
    }

    public LiveData<String> getErrorMessageEvent() {
        return errorMessageEvent;
    }

    public LiveData<Integer> getErrorResEvent() {
        return errorResEvent;
    }

    public LiveData<Boolean> getMessageSentClearInputEvent() {
        return messageSentClearInputEvent;
    }

    public void loadMessages() {
        if (observeDisposable != null && !observeDisposable.isDisposed()) {
            observeDisposable.dispose();
        }
        uiState.setValue(UiState.loading());
        observeDisposable = observeChatMessagesUseCase.execute()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::handleMessagesResult,
                        error -> uiState.setValue(UiState.error(error.getMessage()))
                );
        disposables.add(observeDisposable);
    }

    public void sendMessage(String rawText) {
        if (Boolean.TRUE.equals(sendingMessage.getValue())) {
            return;
        }
        String trimmed = rawText != null ? rawText.trim() : "";
        if (trimmed.isEmpty()) {
            errorResEvent.setValue(R.string.chatbot_empty_message_warning);
            return;
        }
        sendingMessage.setValue(true);
        disposables.add(
                sendChatMessageUseCase.execute(trimmed)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    sendingMessage.setValue(false);
                                    if (result.isSuccess()) {
                                        messageSentClearInputEvent.setValue(true);
                                    } else {
                                        String msg = result.getMessage();
                                        if (msg != null && !msg.isEmpty()) {
                                            errorMessageEvent.setValue(msg);
                                        } else {
                                            errorResEvent.setValue(R.string.chatbot_send_failed);
                                        }
                                    }
                                },
                                error -> {
                                    sendingMessage.setValue(false);
                                    errorResEvent.setValue(R.string.chatbot_send_failed);
                                }
                        )
        );
    }

    private void handleMessagesResult(AppResult<List<ChatMessage>> result) {
        if (result == null) {
            uiState.setValue(UiState.empty());
            return;
        }
        if (result.isError()) {
            uiState.setValue(UiState.error(result.getMessage()));
            return;
        }
        List<ChatMessage> messages = result.getData();
        if (messages == null || messages.isEmpty()) {
            uiState.setValue(UiState.empty());
            return;
        }
        uiState.setValue(UiState.success(new ChatbotUiState(messages)));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
