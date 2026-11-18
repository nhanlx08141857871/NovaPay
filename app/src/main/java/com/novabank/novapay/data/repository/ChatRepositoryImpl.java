package com.novabank.novapay.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.data.dto.ChatRequestDto;
import com.novabank.novapay.data.mapper.ApiErrorParser;
import com.novabank.novapay.data.mapper.ChatMapper;
import com.novabank.novapay.data.remote.server.AiApi;
import com.novabank.novapay.domain.model.chat.ChatMessage;
import com.novabank.novapay.domain.model.chat.ChatReply;
import com.novabank.novapay.domain.repository.ChatRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

@Singleton
public final class ChatRepositoryImpl implements ChatRepository {

    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    private final AiApi aiApi;
    private final ApiErrorParser apiErrorParser;

    @Inject
    public ChatRepositoryImpl(
            FirebaseFirestore firestore,
            FirebaseAuth firebaseAuth,
            AiApi aiApi,
            ApiErrorParser apiErrorParser
    ) {
        this.firestore = firestore;
        this.firebaseAuth = firebaseAuth;
        this.aiApi = aiApi;
        this.apiErrorParser = apiErrorParser;
    }

    @Override
    public Observable<AppResult<List<ChatMessage>>> observeChatMessages() {
        return Observable.<AppResult<List<ChatMessage>>>create(emitter -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser == null) {
                emitter.onNext(AppResult.error(
                        "UNAUTHENTICATED",
                        "Vui lòng đăng nhập để sử dụng trợ lý AI."
                ));
                emitter.onComplete();
                return;
            }
            ListenerRegistration registration = firestore.collection("chat_sessions")
                    .document(currentUser.getUid())
                    .collection("messages")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener((snapshot, error) -> {
                        if (emitter.isDisposed()) {
                            return;
                        }
                        if (error != null) {
                            emitter.onNext(AppResult.error(
                                    "FIRESTORE_ERROR",
                                    error.getMessage() != null
                                            ? error.getMessage()
                                            : "Không thể tải lịch sử trò chuyện."
                            ));
                            return;
                        }
                        List<ChatMessage> messages = new ArrayList<>();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                ChatMessage msg = ChatMapper.toDomainMessage(doc);
                                if (msg != null) {
                                    messages.add(msg);
                                }
                            }
                        }
                        emitter.onNext(AppResult.success(messages));
                    });
            emitter.setCancellable(registration::remove);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Single<AppResult<ChatReply>> sendChatMessage(String message) {
        return aiApi.sendChatMessage(new ChatRequestDto(message))
                .map(dto -> AppResult.success(ChatMapper.toDomainReply(dto)))
                .onErrorReturn(apiErrorParser::toErrorResult)
                .subscribeOn(Schedulers.io());
    }
}
