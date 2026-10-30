package com.moviebooking.modules.movie.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Phân loại phim theo độ tuổi (Thông tư 05/2023/TT-BVHTTDL): P, K, T13, T16, T18. */
@Getter
@RequiredArgsConstructor
public enum AgeRating {
    P(0),
    K(0),
    T13(13),
    T16(16),
    T18(18);

    /** Tuổi tối thiểu được xem (K: dưới 13 tuổi xem cùng cha mẹ — không chặn theo tuổi). */
    private final int minimumAge;
}
