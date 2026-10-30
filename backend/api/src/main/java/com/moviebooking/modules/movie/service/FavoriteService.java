package com.moviebooking.modules.movie.service;

import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import java.util.List;

/** Phim yêu thích (MOV04). */
public interface FavoriteService {

    /** Mới thêm trước; phim đã ngừng chiếu vẫn giữ trong danh sách. */
    List<MovieSummaryResponse> list(Long userId);

    /** Idempotent: thêm lại phim đã có không lỗi. */
    void add(Long userId, Long movieId);

    void remove(Long userId, Long movieId);
}
