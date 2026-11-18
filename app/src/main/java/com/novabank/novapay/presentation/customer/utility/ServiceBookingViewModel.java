package com.novabank.novapay.presentation.customer.utility;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novabank.novapay.core.common.SingleLiveEvent;
import com.novabank.novapay.core.common.UiState;
import com.novabank.novapay.domain.model.account.Account;
import com.novabank.novapay.domain.model.transaction.TransactionDraft;
import com.novabank.novapay.domain.model.transaction.TransactionInitiateRequest;
import com.novabank.novapay.domain.model.transaction.TransactionMeta;
import com.novabank.novapay.domain.model.transaction.TransactionType;
import com.novabank.novapay.domain.model.utility.Order;
import com.novabank.novapay.domain.model.utility.ServiceCategory;
import com.novabank.novapay.domain.model.utility.ServiceItem;
import com.novabank.novapay.domain.usecase.GetActiveCheckingAccountsUseCase;
import com.novabank.novapay.domain.usecase.GetServiceCatalogUseCase;
import com.novabank.novapay.domain.usecase.InitiateTransactionUseCase;
import com.novabank.novapay.domain.usecase.ObserveMyOrdersUseCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel tải danh mục service_catalog, theo dõi orders realtime và đặt dịch vụ qua SERVICE_PAYMENT.
 */
@HiltViewModel
public class ServiceBookingViewModel extends ViewModel {

    private static final int MAX_QUANTITY = 10;

    private final GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase;
    private final GetServiceCatalogUseCase getServiceCatalogUseCase;
    private final ObserveMyOrdersUseCase observeMyOrdersUseCase;
    private final InitiateTransactionUseCase initiateTransactionUseCase;

    private final CompositeDisposable disposables = new CompositeDisposable();
    private Disposable catalogDisposable;

    private final MutableLiveData<UiState<ServiceBookingUiState>> uiState = new MutableLiveData<>();
    private final SingleLiveEvent<TransactionDraft> draftCreatedEvent = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> messageEvent = new SingleLiveEvent<>();

    private List<Account> checkingAccounts = new ArrayList<>();
    private int selectedSourceIndex = 0;
    private boolean showingOrdersTab = false;
    private ServiceCategory activeCategory = ServiceCategory.FLIGHT;
    private List<ServiceItem> catalogItems = new ArrayList<>();
    private List<Order> myOrders = new ArrayList<>();
    private ServiceItem selectedItem = null;
    private int quantity = 1;

    @Inject
    public ServiceBookingViewModel(
            GetActiveCheckingAccountsUseCase getActiveCheckingAccountsUseCase,
            GetServiceCatalogUseCase getServiceCatalogUseCase,
            ObserveMyOrdersUseCase observeMyOrdersUseCase,
            InitiateTransactionUseCase initiateTransactionUseCase
    ) {
        this.getActiveCheckingAccountsUseCase = getActiveCheckingAccountsUseCase;
        this.getServiceCatalogUseCase = getServiceCatalogUseCase;
        this.observeMyOrdersUseCase = observeMyOrdersUseCase;
        this.initiateTransactionUseCase = initiateTransactionUseCase;
    }

    public LiveData<UiState<ServiceBookingUiState>> getUiState() {
        return uiState;
    }

    public SingleLiveEvent<TransactionDraft> getDraftCreatedEvent() {
        return draftCreatedEvent;
    }

    public SingleLiveEvent<String> getMessageEvent() {
        return messageEvent;
    }

    public void loadInitialData() {
        disposables.clear();
        uiState.setValue(UiState.loading());

        disposables.add(
                observeMyOrdersUseCase.execute()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                myOrders = new ArrayList<>(result.getData());
                                publishSuccessStateIfReady();
                            }
                        }, ignored -> {
                        })
        );

        disposables.add(
                getActiveCheckingAccountsUseCase.observe()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (result.isSuccess()) {
                                List<Account> list = result.getData();
                                if (list == null || list.isEmpty()) {
                                    checkingAccounts = new ArrayList<>();
                                    uiState.setValue(UiState.empty());
                                } else {
                                    checkingAccounts = new ArrayList<>(list);
                                    if (selectedSourceIndex >= checkingAccounts.size()) {
                                        selectedSourceIndex = 0;
                                    }
                                    loadCategoryCatalog(activeCategory);
                                }
                            } else {
                                uiState.setValue(UiState.error(result.getMessage()));
                            }
                        }, throwable -> uiState.setValue(UiState.error(throwable.getMessage())))
        );
    }

    public void selectSourceAccount(int index) {
        if (index >= 0 && index < checkingAccounts.size()) {
            selectedSourceIndex = index;
            publishSuccessState();
        }
    }

    public void onTabSelected(int tabPosition) {
        if (tabPosition == 4) {
            showingOrdersTab = true;
            selectedItem = null;
            publishSuccessState();
            return;
        }
        showingOrdersTab = false;
        ServiceCategory nextCategory = switch (tabPosition) {
            case 1 -> ServiceCategory.MOVIE;
            case 2 -> ServiceCategory.HOTEL;
            case 3 -> ServiceCategory.PRODUCT;
            default -> ServiceCategory.FLIGHT;
        };
        if (activeCategory != nextCategory || catalogItems.isEmpty()) {
            activeCategory = nextCategory;
            selectedItem = null;
            quantity = 1;
            loadCategoryCatalog(nextCategory);
        } else {
            publishSuccessState();
        }
    }

    private void loadCategoryCatalog(@NonNull ServiceCategory category) {
        if (checkingAccounts.isEmpty()) {
            return;
        }
        if (catalogDisposable != null && !catalogDisposable.isDisposed()) {
            catalogDisposable.dispose();
        }
        uiState.setValue(UiState.loading());
        catalogDisposable = getServiceCatalogUseCase.execute(category)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isSuccess()) {
                        catalogItems = result.getData() != null
                                ? new ArrayList<>(result.getData())
                                : new ArrayList<>();
                        publishSuccessState();
                    } else {
                        publishSuccessState();
                        messageEvent.setValue(result.getMessage());
                    }
                }, throwable -> {
                    publishSuccessState();
                    messageEvent.setValue(throwable.getMessage());
                });
        disposables.add(catalogDisposable);
    }

    public void selectServiceItem(@NonNull ServiceItem item) {
        selectedItem = item;
        quantity = 1;
        publishSuccessState();
    }

    public void changeQuantity(int delta) {
        if (selectedItem == null) {
            return;
        }
        int next = Math.max(1, Math.min(MAX_QUANTITY, quantity + delta));
        if (next != quantity) {
            quantity = next;
            publishSuccessState();
        }
    }

    public void submitServiceBooking(@NonNull String note) {
        if (checkingAccounts.isEmpty() || selectedItem == null) {
            return;
        }
        Account sourceAccount = checkingAccounts.get(selectedSourceIndex);
        long totalAmount = Math.multiplyExact(selectedItem.getPrice(), (long) quantity);

        TransactionInitiateRequest request = new TransactionInitiateRequest(
                TransactionType.SERVICE_PAYMENT,
                sourceAccount.getAccountId(),
                null,
                null,
                totalAmount,
                note,
                TransactionMeta.forServiceQuantity(selectedItem.getItemId(), quantity)
        );

        uiState.setValue(UiState.loading());
        disposables.add(
                initiateTransactionUseCase.execute(request, sourceAccount)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            publishSuccessState();
                            if (result.isSuccess() && result.getData() != null) {
                                draftCreatedEvent.setValue(result.getData());
                            } else {
                                messageEvent.setValue(result.getMessage());
                            }
                        }, throwable -> {
                            publishSuccessState();
                            messageEvent.setValue(throwable.getMessage());
                        })
        );
    }

    private void publishSuccessStateIfReady() {
        if (!checkingAccounts.isEmpty()) {
            publishSuccessState();
        }
    }

    private void publishSuccessState() {
        if (checkingAccounts.isEmpty()) {
            return;
        }
        uiState.setValue(UiState.success(new ServiceBookingUiState(
                checkingAccounts,
                selectedSourceIndex,
                showingOrdersTab,
                activeCategory,
                catalogItems,
                myOrders,
                selectedItem,
                quantity
        )));
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
