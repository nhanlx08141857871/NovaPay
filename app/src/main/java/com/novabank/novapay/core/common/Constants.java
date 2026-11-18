package com.novabank.novapay.core.common;

/**
 * Tập hợp các hằng số dùng chung toàn hệ thống NovaPay (DeepLinks, FragmentResult keys,
 * ngưỡng bảo mật giao dịch, tọa độ fallback ĐH Tôn Đức Thắng và URL máy chủ).
 */
public final class Constants {

    private Constants() {
        // Cấm khởi tạo
    }

    // --- Cấu hình kết nối API ---
    public static final String DEFAULT_BACKEND_BASE_URL = "http://10.0.2.2:3000/";
    public static final String OPEN_ROUTE_BASE_URL = "https://api.openrouteservice.org/";
    public static final long NETWORK_TIMEOUT_SECONDS = 30L;

    // --- Múi giờ & Tiền tệ ---
    public static final String ZONE_HO_CHI_MINH = "Asia/Ho_Chi_Minh";
    public static final String CURRENCY_VND = "VND";

    // --- Ràng buộc Bảo mật Giao dịch (SPEC_CHOT.md) ---
    public static final long HIGH_VALUE_THRESHOLD_VND = 5_000_000L;
    public static final int OTP_EXPIRY_SECONDS = 180;
    public static final int OTP_MAX_ATTEMPTS = 5;
    public static final int MAX_EKYC_IMAGE_BYTES = 200 * 1024;

    // --- Vai trò & Trạng thái Người dùng ---
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_OFFICER = "OFFICER";
    public static final String USER_STATUS_ACTIVE = "ACTIVE";
    public static final String USER_STATUS_LOCKED = "LOCKED";
    public static final String EKYC_STATUS_VERIFIED = "VERIFIED";
    public static final String EKYC_STATUS_PENDING = "PENDING";
    public static final String EKYC_STATUS_REJECTED = "REJECTED";

    // --- Tọa độ Fallback Bản đồ: Trường ĐH Tôn Đức Thắng (Quận 7, TP.HCM) ---
    public static final double TDTU_DEFAULT_LAT = 10.7326;
    public static final double TDTU_DEFAULT_LNG = 106.6998;
    public static final double MAX_VALID_DISTANCE_FROM_HCMC_METERS = 100_000.0;
    public static final long GPS_TIMEOUT_MILLIS = 5_000L;

    // --- Bảng Định Tuyến DeepLinks (novapay://) ---
    public static final String DEEPLINK_SCHEME = "novapay";
    public static final String DEEPLINK_HOME = "novapay://home";
    public static final String DEEPLINK_ACCOUNT_PREFIX = "novapay://accounts/";
    public static final String DEEPLINK_TRANSACTIONS_MONTH = "novapay://transactions?range=month";
    public static final String DEEPLINK_PROFILE = "novapay://profile";
    public static final String DEEPLINK_EKYC = "novapay://ekyc";
    public static final String DEEPLINK_TRANSFER = "novapay://transfer";
    public static final String DEEPLINK_BILLS = "novapay://bills";
    public static final String DEEPLINK_TOPUP = "novapay://topup";
    public static final String DEEPLINK_SERVICES = "novapay://services";
    public static final String DEEPLINK_BRANCHES = "novapay://branches";

    // --- Khóa giao tiếp FragmentResultListener liên phân hệ (A <-> B <-> C) ---
    public static final String REQ_FACE_CHECK = "req_novapay_face_check";
    public static final String REQ_OTP_VERIFICATION = "req_novapay_otp_verification";
    public static final String REQ_EKYC_CAPTURE = "req_novapay_ekyc_capture";

    public static final String BUNDLE_DRAFT_ID = "bundle_draft_id";
    public static final String BUNDLE_FACE_CHECK_PASSED = "bundle_face_check_passed";
    public static final String BUNDLE_OTP_EXPIRES_AT_EPOCH_MS = "bundle_otp_expires_at_epoch_ms";
    public static final String BUNDLE_OTP_CODE = "bundle_otp_code";
    public static final String BUNDLE_OTP_VERIFIED = "bundle_otp_verified";
    public static final String BUNDLE_TX_ID = "bundle_tx_id";
    public static final String BUNDLE_CLIENT_SECRET = "bundle_client_secret";
    public static final String BUNDLE_TARGET_UID = "bundle_target_uid";
}
