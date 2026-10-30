package com.moviebooking.common.util;

public final class Strings {

    private Strings() {}

    /** Chuỗi rỗng/chỉ có khoảng trắng → null, ngược lại cắt khoảng trắng hai đầu. */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
