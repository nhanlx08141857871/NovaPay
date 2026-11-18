package com.novabank.novapay.core.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Lớp cơ sở bắt buộc cho mọi Fragment trong dự án NovaPay.
 * Tự động quản lý vòng đời ViewBinding (chống rò rỉ bộ nhớ) và CompositeDisposable của RxJava 3.
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    private VB binding;
    protected final CompositeDisposable disposables = new CompositeDisposable();

    @NonNull
    protected abstract VB inflateBinding(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container
    );

    protected abstract void setupViews();

    protected abstract void observeViewModel();

    @NonNull
    protected final VB getBinding() {
        if (binding == null) {
            throw new IllegalStateException(
                    "ViewBinding accessed outside Fragment view lifecycle in " + getClass().getSimpleName()
            );
        }
        return binding;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = inflateBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews();
        observeViewModel();
    }

    protected final void addDisposable(@NonNull Disposable disposable) {
        disposables.add(disposable);
    }

    /**
     * Điều hướng liên phân hệ an toàn bằng Internal DeepLink (novapay://...) theo SPEC_CHOT.md Q4.
     */
    protected final void navigateDeepLink(@NonNull String deepLinkUri) {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(Uri.parse(deepLinkUri));
    }

    protected final void showSnackbar(@NonNull String message) {
        if (binding != null && !message.isEmpty()) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        binding = null;
        super.onDestroyView();
    }
}
