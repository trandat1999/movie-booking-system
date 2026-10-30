package com.moviebooking.modules.movie.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.movie.dto.request.MovieRequest;
import com.moviebooking.modules.movie.dto.response.MovieDetailResponse;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import java.util.List;

/** Quản lý phim (MOVADM01–04, 08–10). */
public interface MovieService {

    PageResponse<MovieSummaryResponse> search(String keyword, MovieStatus status, Long genreId, int page, int size);

    MovieDetailResponse get(Long id);

    MovieDetailResponse create(MovieRequest request);

    MovieDetailResponse update(Long id, MovieRequest request);

    /** Chỉ xóa được phim chưa có suất chiếu; phim đã chiếu thì chuyển trạng thái ENDED. */
    void delete(Long id);

    // --- Khách hàng (MOV01–MOV05) ---

    /** Chỉ phim đang chiếu/sắp chiếu; {@code status} null = cả hai. Sắp chiếu xếp theo ngày khởi chiếu gần nhất. */
    PageResponse<MovieSummaryResponse> searchPublic(
            String keyword, MovieStatus status, Long genreId, AgeRating ageRating, String language, int page, int size);

    /** Phim đã ngừng chiếu trả về MOVIE_NOT_FOUND. */
    MovieDetailResponse getPublicBySlug(String slug);

    List<String> listLanguages();

    /** Dùng cho module lịch chiếu. */
    Movie getEntity(Long id);
}
