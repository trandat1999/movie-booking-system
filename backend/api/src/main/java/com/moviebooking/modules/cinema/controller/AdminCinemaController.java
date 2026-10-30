package com.moviebooking.modules.cinema.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.cinema.dto.request.CinemaRequest;
import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import com.moviebooking.modules.cinema.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Rạp")
@RestController
@RequestMapping("/api/v1/admin/cinemas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('cinema:write', 'showtime:write')")
public class AdminCinemaController {

    private final CinemaService cinemaService;

    @Operation(summary = "Danh sách rạp (CIN01)")
    @GetMapping
    public ApiResponse<List<CinemaResponse>> list() {
        return ApiResponse.ok(cinemaService.list());
    }

    @Operation(summary = "Chi tiết rạp")
    @GetMapping("/{id}")
    public ApiResponse<CinemaResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(cinemaService.get(id));
    }

    @Operation(summary = "Thêm rạp (CIN02)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('cinema:write')")
    @Audited(action = "CINEMA_CREATE", resource = "cinema", data = "#request")
    public ApiResponse<CinemaResponse> create(@Valid @RequestBody CinemaRequest request) {
        return ApiResponse.ok(cinemaService.create(request));
    }

    @Operation(summary = "Sửa rạp / ngừng hoạt động (CIN02)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cinema:write')")
    @Audited(action = "CINEMA_UPDATE", resource = "cinema", resourceId = "#id", data = "#request")
    public ApiResponse<CinemaResponse> update(@PathVariable Long id, @Valid @RequestBody CinemaRequest request) {
        return ApiResponse.ok(cinemaService.update(id, request));
    }
}
