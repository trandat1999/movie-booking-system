package com.moviebooking.modules.movie.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.movie.dto.request.MovieRequest;
import com.moviebooking.modules.movie.dto.response.MovieDetailResponse;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Phim")
@RestController
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('movie:write', 'showtime:write')")
public class AdminMovieController {

    private final MovieService movieService;

    @Operation(summary = "Danh sách phim, tìm theo tên, lọc trạng thái/thể loại (MOVADM01)")
    @GetMapping
    public ApiResponse<PageResponse<MovieSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MovieStatus status,
            @RequestParam(required = false) Long genreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(movieService.search(q, status, genreId, page, size));
    }

    @Operation(summary = "Chi tiết phim")
    @GetMapping("/{id}")
    public ApiResponse<MovieDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(movieService.get(id));
    }

    @Operation(summary = "Thêm phim (MOVADM02)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('movie:write')")
    @Audited(action = "MOVIE_CREATE", resource = "movie", data = "#request")
    public ApiResponse<MovieDetailResponse> create(@Valid @RequestBody MovieRequest request) {
        return ApiResponse.ok(movieService.create(request));
    }

    @Operation(summary = "Cập nhật phim (MOVADM03)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('movie:write')")
    @Audited(action = "MOVIE_UPDATE", resource = "movie", resourceId = "#id", data = "#request")
    public ApiResponse<MovieDetailResponse> update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return ApiResponse.ok(movieService.update(id, request));
    }

    @Operation(summary = "Xóa phim chưa có suất chiếu (MOVADM04)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('movie:write')")
    @Audited(action = "MOVIE_DELETE", resource = "movie", resourceId = "#id")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        movieService.delete(id);
        return ApiResponse.ok();
    }
}
