package com.novabank.novapay;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.novabank.novapay.core.common.Constants;
import com.novabank.novapay.core.security.SessionManager;
import com.novabank.novapay.databinding.ActivityMainBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Single-Activity trung tâm chứa NavHostFragment, thanh BottomNavigationView 4 tab cho Customer,
 * bảo vệ DeepLink Auth/Role Guard và xử lý cưỡng bức đăng xuất realtime khi tài khoản bị LOCKED.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    SessionManager sessionManager;

    private ActivityMainBinding binding;
    private NavController navController;
    private boolean isRedirecting = false;
    private boolean isSyncingBottomNav = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            binding.bottomNavCustomer.setPadding(0, 0, 0, 0);
            return insets;
        });

        setupNavigationAndGuards();
        setupCustomerBottomNav();
        observeSessionLock();
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void setupNavigationAndGuards() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.navHostFragment);
        if (navHostFragment == null) {
            return;
        }
        navController = navHostFragment.getNavController();
        navController.addOnDestinationChangedListener(this::enforceDestinationGuard);
    }

    private void setupCustomerBottomNav() {
        binding.bottomNavCustomer.setOnItemSelectedListener(item -> {
            if (isSyncingBottomNav || navController == null || !sessionManager.isSignedIn()) {
                return true;
            }
            int itemId = item.getItemId();
            String targetDeepLink;
            if (itemId == R.id.nav_tab_home) {
                targetDeepLink = Constants.DEEPLINK_HOME;
            } else if (itemId == R.id.nav_tab_transfer) {
                targetDeepLink = Constants.DEEPLINK_TRANSFER;
            } else if (itemId == R.id.nav_tab_history) {
                targetDeepLink = Constants.DEEPLINK_TRANSACTIONS_MONTH;
            } else if (itemId == R.id.nav_tab_profile) {
                targetDeepLink = Constants.DEEPLINK_PROFILE;
            } else {
                return false;
            }

            NavOptions tabOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(R.id.customerHomeFragment, false, true)
                    .build();
            isRedirecting = true;
            try {
                navController.navigate(Uri.parse(targetDeepLink), tabOptions);
                return true;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                showMessage(getString(R.string.core_guard_destination_unavailable));
                return false;
            } finally {
                isRedirecting = false;
            }
        });

        binding.bottomNavCustomer.setOnItemReselectedListener(item -> {
            // Tránh tạo lại Fragment khi người dùng chạm lại tab đang mở
        });
    }

    private void enforceDestinationGuard(
            @NonNull NavController controller,
            @NonNull NavDestination destination,
            @Nullable Bundle arguments
    ) {
        int destId = destination.getId();
        if (destId == R.id.loginFragment) {
            updateBottomNavState(destination, false, true);
            return;
        }

        if (!isRedirecting && !sessionManager.isSignedIn()) {
            updateBottomNavState(destination, false, true);
            redirectToLogin();
            return;
        }

        String role = sessionManager.getCachedRole();
        boolean isOfficer = Constants.ROLE_OFFICER.equalsIgnoreCase(role);

        if (!isRedirecting && isOfficer && isCustomerOnlyDestination(destId)) {
            updateBottomNavState(destination, false, true);
            showMessage(getString(R.string.core_guard_role_denied));
            redirectToRoleHome(role);
            return;
        }

        if (!isRedirecting && !isOfficer && destId == R.id.ekycCaptureFragment && arguments != null) {
            String targetUid = arguments.getString("targetUid");
            if (targetUid == null) {
                targetUid = arguments.getString(Constants.BUNDLE_TARGET_UID);
            }
            String currentUid = sessionManager.getCurrentUid();
            if (targetUid != null && !targetUid.trim().isEmpty()
                    && (currentUid == null || !targetUid.trim().equals(currentUid))) {
                updateBottomNavState(destination, false, isOfficer);
                showMessage(getString(R.string.core_guard_role_denied));
                redirectToRoleHome(role);
                return;
            }
        }

        updateBottomNavState(destination, sessionManager.isSignedIn(), isOfficer);
    }

    private void updateBottomNavState(
            @NonNull NavDestination destination,
            boolean isSignedIn,
            boolean isOfficer
    ) {
        if (binding == null) {
            return;
        }
        int selectedTabId = resolveCustomerTabId(destination);
        boolean showBottomNav = isSignedIn && !isOfficer && selectedTabId != 0;

        int visibility = showBottomNav ? View.VISIBLE : View.GONE;
        binding.bottomNavCustomer.setVisibility(visibility);
        binding.viewBottomNavDivider.setVisibility(visibility);

        if (showBottomNav && binding.bottomNavCustomer.getSelectedItemId() != selectedTabId) {
            isSyncingBottomNav = true;
            try {
                binding.bottomNavCustomer.setSelectedItemId(selectedTabId);
            } finally {
                isSyncingBottomNav = false;
            }
        }
    }

    private int resolveCustomerTabId(@NonNull NavDestination destination) {
        int destId = destination.getId();
        if (destId == R.id.customerHomeFragment) {
            return R.id.nav_tab_home;
        }
        if (destId == R.id.transactionHistoryFragment) {
            return R.id.nav_tab_history;
        }
        if (destId == R.id.profileFragment) {
            return R.id.nav_tab_profile;
        }
        if (destination.hasDeepLink(Uri.parse(Constants.DEEPLINK_TRANSFER))) {
            return R.id.nav_tab_transfer;
        }
        return 0;
    }

    private boolean isCustomerOnlyDestination(int destId) {
        return destId == R.id.customerHomeFragment
                || destId == R.id.accountRouterFragment
                || destId == R.id.checkingDetailFragment
                || destId == R.id.savingDetailFragment
                || destId == R.id.mortgageDetailFragment
                || destId == R.id.transactionHistoryFragment
                || destId == R.id.faceCheckDialogFragment;
    }

    private void handleIncomingIntent(@Nullable Intent intent) {
        if (intent == null || navController == null) {
            return;
        }
        Uri data = intent.getData();
        if (data == null || !Constants.DEEPLINK_SCHEME.equalsIgnoreCase(data.getScheme())) {
            return;
        }

        if (!sessionManager.isSignedIn()) {
            sessionManager.setPendingDeepLink(data);
            redirectToLogin();
            return;
        }

        String role = sessionManager.getCachedRole();
        String uid = sessionManager.getCurrentUid();
        if (!SessionManager.isDeepLinkAllowedForRole(data.toString(), role, uid)) {
            showMessage(getString(R.string.core_guard_role_denied));
            redirectToRoleHome(role);
            return;
        }

        navigateUriSafely(data);
    }

    private void observeSessionLock() {
        sessionManager.getAccountLockedEvent().observe(this, locked -> {
            if (Boolean.TRUE.equals(locked)) {
                showMessage(getString(R.string.core_session_locked_message));
                redirectToLogin();
            }
        });
    }

    private void redirectToLogin() {
        if (navController == null) {
            return;
        }
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == R.id.loginFragment) {
            return;
        }
        isRedirecting = true;
        try {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .setLaunchSingleTop(true)
                    .build();
            navController.navigate(R.id.loginFragment, null, options);
        } finally {
            isRedirecting = false;
        }
    }

    private void redirectToRoleHome(@NonNull String role) {
        String target = Constants.ROLE_OFFICER.equalsIgnoreCase(role)
                ? SessionManager.DEEPLINK_OFFICER
                : Constants.DEEPLINK_HOME;
        navigateUriSafely(Uri.parse(target));
    }

    private void navigateUriSafely(@NonNull Uri uri) {
        if (navController == null) {
            return;
        }
        isRedirecting = true;
        try {
            navController.navigate(uri);
            if (navController.getCurrentDestination() != null) {
                String role = sessionManager.getCachedRole();
                updateBottomNavState(
                        navController.getCurrentDestination(),
                        sessionManager.isSignedIn(),
                        Constants.ROLE_OFFICER.equalsIgnoreCase(role)
                );
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showMessage(getString(R.string.core_guard_destination_unavailable));
        } finally {
            isRedirecting = false;
        }
    }

    private void showMessage(@NonNull String message) {
        if (binding != null && !message.isEmpty()) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        }
    }
}
