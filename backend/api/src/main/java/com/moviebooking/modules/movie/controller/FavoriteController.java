package com.moviebooking.modules.movie.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.service.FavoriteService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Phim yêu thích")
@RestController
@RequestMapping("/api/v1/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Phim yêu thích của tôi (MOV04)")
    @GetMapping
    public ApiResponse<List<MovieSummaryResponse>> list(@CurrentUser AuthenticatedUser user) {
        return ApiResponse.ok(favoriteService.list(user.id()));
    }

    @Operation(summary = "Thêm phim yêu thích")
    @PutMapping("/{movieId}")
    public ApiResponse<Void> add(@CurrentUser AuthenticatedUser user, @PathVariable Long movieId) {
        favoriteService.add(user.id(), movieId);
        return ApiResponse.ok();
    }

    @Operation(summary = "Bỏ phim yêu thích")
    @DeleteMapping("/{movieId}")
    public ApiResponse<Void> remove(@CurrentUser AuthenticatedUser user, @PathVariable Long movieId) {
        favoriteService.remove(user.id(), movieId);
        return ApiResponse.ok();
    }
}
