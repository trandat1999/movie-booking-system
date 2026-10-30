package com.moviebooking.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

    @Test
    void removesVietnameseDiacritics() {
        assertThat(Slugs.of("Mùa Hè Đỏ Lửa")).isEqualTo("mua-he-do-lua");
        assertThat(Slugs.of("  Đường Đua: Sấm Sét 2!  ")).isEqualTo("duong-dua-sam-set-2");
        assertThat(Slugs.of("Khoa học viễn tưởng")).isEqualTo("khoa-hoc-vien-tuong");
    }

    @Test
    void fallsBackWhenNothingLeft() {
        assertThat(Slugs.of("!!!")).isEqualTo("item");
    }
}
