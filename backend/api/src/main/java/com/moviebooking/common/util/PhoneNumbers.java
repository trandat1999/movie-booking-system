package com.moviebooking.common.util;

/** Chuẩn hóa số điện thoại Việt Nam về dạng {@code 0xxxxxxxxx}. */
public final class PhoneNumbers {

    /** Số di động Việt Nam: 0 hoặc +84, tiếp theo đầu số 3/5/7/8/9 và 8 chữ số. */
    public static final String VN_MOBILE_PATTERN = "^(0|\\+84)(3|5|7|8|9)\\d{8}$";

    private PhoneNumbers() {}

    public static String normalize(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.replaceAll("[\\s.-]", "");
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.startsWith("+84") ? "0" + trimmed.substring(3) : trimmed;
    }
}
