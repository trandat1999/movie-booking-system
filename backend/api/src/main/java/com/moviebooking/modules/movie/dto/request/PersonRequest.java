package com.moviebooking.modules.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonRequest(
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
                String fullName,
        @Size(max = 500, message = "Đường dẫn ảnh quá dài") String avatarUrl) {}
