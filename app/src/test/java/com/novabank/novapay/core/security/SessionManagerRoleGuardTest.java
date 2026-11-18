package com.novabank.novapay.core.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.novabank.novapay.core.common.Constants;

import org.junit.Test;

public class SessionManagerRoleGuardTest {

    @Test
    public void customer_allowedForCustomerRoutes_blockedFromOfficerAndOtherTargetEkyc() {
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_HOME, Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole("novapay://accounts/acc_1", Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_TRANSACTIONS_MONTH, Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_TRANSFER, Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_PROFILE, Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_BRANCHES, Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole("novapay://chatbot", Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole("novapay://face-check?draftId=d1", Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_EKYC, Constants.ROLE_CUSTOMER, "u1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole("novapay://ekyc?targetUid=u1", Constants.ROLE_CUSTOMER, "u1"));

        assertFalse(SessionManager.isDeepLinkAllowedForRole("novapay://officer", Constants.ROLE_CUSTOMER, "u1"));
        assertFalse(SessionManager.isDeepLinkAllowedForRole("novapay://ekyc?targetUid=u2", Constants.ROLE_CUSTOMER, "u1"));
        assertFalse(SessionManager.isDeepLinkAllowedForRole("novapay://ekyc?targetUid=u2", Constants.ROLE_CUSTOMER));
    }

    @Test
    public void officer_allowedForOfficerBranchesProfileCounterEkyc_blockedFromCustomerMoney() {
        assertTrue(SessionManager.isDeepLinkAllowedForRole("novapay://officer", Constants.ROLE_OFFICER, "off1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_BRANCHES, Constants.ROLE_OFFICER, "off1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_PROFILE, Constants.ROLE_OFFICER, "off1"));
        assertTrue(SessionManager.isDeepLinkAllowedForRole("novapay://ekyc?targetUid=cust2", Constants.ROLE_OFFICER, "off1"));

        assertFalse(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_HOME, Constants.ROLE_OFFICER, "off1"));
        assertFalse(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_TRANSFER, Constants.ROLE_OFFICER, "off1"));
        assertFalse(SessionManager.isDeepLinkAllowedForRole(Constants.DEEPLINK_BILLS, Constants.ROLE_OFFICER, "off1"));
        assertFalse(SessionManager.isDeepLinkAllowedForRole("novapay://accounts/acc_1", Constants.ROLE_OFFICER, "off1"));
        assertFalse(SessionManager.isDeepLinkAllowedForRole("novapay://face-check?draftId=d1", Constants.ROLE_OFFICER, "off1"));
    }

    @Test
    public void invalidSchemeOrUnknownHost_rejectedForAllRoles() {
        assertFalse(SessionManager.isDeepLinkAllowedForRole("https://novapay.vn/home", Constants.ROLE_CUSTOMER));
        assertFalse(SessionManager.isDeepLinkAllowedForRole("novapay://unknown", Constants.ROLE_CUSTOMER));
        assertFalse(SessionManager.isDeepLinkAllowedForRole("", Constants.ROLE_CUSTOMER));
        assertFalse(SessionManager.isDeepLinkAllowedForRole(null, Constants.ROLE_OFFICER));
    }
}
