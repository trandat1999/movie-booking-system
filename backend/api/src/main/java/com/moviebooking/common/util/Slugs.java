package com.moviebooking.common.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Tạo slug URL từ chuỗi tiếng Việt, ví dụ "Mùa Hè Đỏ Lửa" → "mua-he-do-lua". */
public final class Slugs {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private Slugs() {}

    public static String of(String text) {
        String withoutMarks = DIACRITICS
                .matcher(Normalizer.normalize(text, Normalizer.Form.NFD))
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String slug = NON_ALPHANUMERIC
                .matcher(withoutMarks.toLowerCase(Locale.ROOT))
                .replaceAll("-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isEmpty() ? "item" : slug;
    }
}
