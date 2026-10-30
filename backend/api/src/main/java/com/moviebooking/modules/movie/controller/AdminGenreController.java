package com.moviebooking.modules.movie.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.movie.dto.request.GenreRequest;
import com.moviebooking.modules.movie.dto.response.GenreResponse;
import com.moviebooking.modules.movie.service.GenreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Thể loại phim")
@RestController
@RequestMapping("/api/v1/admin/genres")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('movie:write')")
public class AdminGenreController {

    private final GenreService genreService;

    @Operation(summary = "Danh sách thể loại")
    @GetMapping
    public ApiResponse<List<GenreResponse>> list() {
        return ApiResponse.ok(genreService.list());
    }

    @Operation(summary = "Thêm thể loại")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(action = "GENRE_CREATE", resource = "genre", data = "#request")
    public ApiResponse<GenreResponse> create(@Valid @RequestBody GenreRequest request) {
        return ApiResponse.ok(genreService.create(request));
    }

    @Operation(summary = "Sửa thể loại")
    @PutMapping("/{id}")
    @Audited(action = "GENRE_UPDATE", resource = "genre", resourceId = "#id", data = "#request")
    public ApiResponse<GenreResponse> update(@PathVariable Long id, @Valid @RequestBody GenreRequest request) {
        return ApiResponse.ok(genreService.update(id, request));
    }

    @Operation(summary = "Xóa thể loại chưa được dùng")
    @DeleteMapping("/{id}")
    @Audited(action = "GENRE_DELETE", resource = "genre", resourceId = "#id")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        genreService.delete(id);
        return ApiResponse.ok();
    }
}
