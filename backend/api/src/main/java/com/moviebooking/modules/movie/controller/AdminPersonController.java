package com.moviebooking.modules.movie.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.movie.dto.request.PersonRequest;
import com.moviebooking.modules.movie.dto.response.PersonResponse;
import com.moviebooking.modules.movie.service.PersonService;
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

@Tag(name = "Admin - Đạo diễn & diễn viên")
@RestController
@RequestMapping("/api/v1/admin/people")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('movie:write')")
public class AdminPersonController {

    private final PersonService personService;

    @Operation(summary = "Tìm đạo diễn/diễn viên theo tên")
    @GetMapping
    public ApiResponse<PageResponse<PersonResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(personService.search(q, page, size));
    }

    @Operation(summary = "Thêm người")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(action = "PERSON_CREATE", resource = "person", data = "#request")
    public ApiResponse<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
        return ApiResponse.ok(personService.create(request));
    }

    @Operation(summary = "Sửa người")
    @PutMapping("/{id}")
    @Audited(action = "PERSON_UPDATE", resource = "person", resourceId = "#id", data = "#request")
    public ApiResponse<PersonResponse> update(@PathVariable Long id, @Valid @RequestBody PersonRequest request) {
        return ApiResponse.ok(personService.update(id, request));
    }

    @Operation(summary = "Xóa người chưa gắn với phim")
    @DeleteMapping("/{id}")
    @Audited(action = "PERSON_DELETE", resource = "person", resourceId = "#id")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return ApiResponse.ok();
    }
}
