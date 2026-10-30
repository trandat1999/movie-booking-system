package com.moviebooking.modules.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenreRequest(
        @NotBlank(message = "Vui lòng nhập tên thể loại") @Size(max = 50, message = "Tên thể loại tối đa 50 ký tự")
                String name) {}
