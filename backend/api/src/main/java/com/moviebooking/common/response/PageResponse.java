package com.moviebooking.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/** Kết quả phân trang. {@code page} bắt đầu từ 0, giống {@link org.springframework.data.domain.Pageable}. */
public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
