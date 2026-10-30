package com.moviebooking.modules.movie.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.movie.dto.response.GenreResponse;
import com.moviebooking.modules.movie.dto.response.MovieDetailResponse;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.service.GenreService;
import com.moviebooking.modules.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API công khai cho khách: tra cứu phim (không cần đăng nhập). */
@Tag(name = "Phim")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;
    private final GenreService genreService;

    @Operation(summary = "Danh sách phim đang chiếu / sắp chiếu, tìm và lọc (MOV02–MOV04, MOV09)")
    @GetMapping("/movies")
    public ApiResponse<PageResponse<MovieSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) AgeRating ageRating,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.ok(movieService.searchPublic(q, status, genreId, ageRating, language, page, size));
    }

    @Operation(summary = "Chi tiết phim theo slug (MOV05, MOV06)")
    @GetMapping("/movies/{slug}")
    public ApiResponse<MovieDetailResponse> get(@PathVariable String slug) {
        return ApiResponse.ok(movieService.getPublicBySlug(slug));
    }

    @Operation(summary = "Các ngôn ngữ phim — dùng cho bộ lọc")
    @GetMapping("/movie-languages")
    public ApiResponse<List<String>> languages() {
        return ApiResponse.ok(movieService.listLanguages());
    }

    @Operation(summary = "Danh sách thể loại")
    @GetMapping("/genres")
    public ApiResponse<List<GenreResponse>> genres() {
        return ApiResponse.ok(genreService.list());
    }
}
