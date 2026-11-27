package com.novabank.novapay.presentation.customer.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.AppResult;
import com.novabank.novapay.domain.model.security.SecurityStatus;
import com.novabank.novapay.domain.model.user.EkycInfo;
import com.novabank.novapay.domain.model.user.EkycStatus;
import com.novabank.novapay.domain.model.user.User;
import com.novabank.novapay.domain.model.user.UserRole;
import com.novabank.novapay.domain.model.user.UserSecurityInfo;
import com.novabank.novapay.domain.model.user.UserStatus;
import com.novabank.novapay.presentation.customer.account.OpenSavingViewModel;

import org.junit.Test;

import java.time.Instant;
import java.util.Collections;

public class PinFallbackResolutionTest {

    private static User createUser(boolean hasPin) {
        return new User(
                "u1",
                UserRole.CUSTOMER,
                "Test User",
                "user@example.com",
                "0901234567",
                "079099000001",
                "1999-01-01",
                "TP.HCM",
                "",
                UserStatus.ACTIVE,
                new EkycInfo(EkycStatus.VERIFIED, "off1", Instant.now()),
                new UserSecurityInfo(hasPin, hasPin ? Instant.now() : null),
                "off1",
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    public void openSavingPinResolution_fallsBackToUserProfileAndPinRequiredOverride() {
        AppResult<SecurityStatus> statusError = AppResult.error("NETWORK_ERROR", "offline");
        AppResult<User> userWithPin = AppResult.success(createUser(true));
        AppResult<User> userWithoutPin = AppResult.success(createUser(false));

        // Status succeeded -> authoritative
        assertFalse(OpenSavingViewModel.resolveHasTransactionPin(
                AppResult.success(new SecurityStatus(false, null)),
                userWithPin,
                false
        ));
        assertTrue(OpenSavingViewModel.resolveHasTransactionPin(
                AppResult.success(new SecurityStatus(true, null)),
                userWithoutPin,
                false
        ));

        // Status failed -> falls back to user.getSecurity().hasPin()
        assertTrue(OpenSavingViewModel.resolveHasTransactionPin(statusError, userWithPin, false));
        assertFalse(OpenSavingViewModel.resolveHasTransactionPin(statusError, userWithoutPin, false));

        // PIN_REQUIRED override forces PIN input visible regardless of earlier fetch failures
        assertTrue(OpenSavingViewModel.resolveHasTransactionPin(statusError, userWithoutPin, true));
    }

    @Test
    public void securitySettingsUiState_showsControlsPinWhenStatusUnloadedOrPinSet() {
        User user = createUser(false);
        SecuritySettingsUiState loadedNoPin = new SecuritySettingsUiState(
                user,
                new SecurityStatus(false, null),
                Collections.emptyList(),
                null,
                true,
                false
        );
        assertFalse(loadedNoPin.shouldShowControlsPin());

        SecuritySettingsUiState unloadedFallback = new SecuritySettingsUiState(
                user,
                new SecurityStatus(false, null),
                Collections.emptyList(),
                null,
                false,
                false
        );
        assertTrue(unloadedFallback.shouldShowControlsPin());

        SecuritySettingsUiState loadedWithPin = new SecuritySettingsUiState(
                user,
                new SecurityStatus(true, null),
                Collections.emptyList(),
                null,
                true,
                false
        );
        assertTrue(loadedWithPin.shouldShowControlsPin());
    }

    @Test
    public void pinErrorCodes_triggerPinInputFallback() {
        assertTrue(OpenSavingViewModel.isPinErrorCode("PIN_REQUIRED"));
        assertTrue(OpenSavingViewModel.isPinErrorCode("PIN_INVALID"));
        assertTrue(OpenSavingViewModel.isPinErrorCode("PIN_LOCKED"));
        assertFalse(OpenSavingViewModel.isPinErrorCode("INSUFFICIENT_FUNDS"));

        assertTrue(SecuritySettingsViewModel.isPinErrorCode("PIN_REQUIRED"));
        assertTrue(SecuritySettingsViewModel.isPinErrorCode("pin_invalid"));
        assertFalse(SecuritySettingsViewModel.isPinErrorCode("INVALID_ARGUMENT"));
    }
}
