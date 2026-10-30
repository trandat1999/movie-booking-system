package com.moviebooking.modules.cinema.dto.request;

import com.moviebooking.modules.cinema.enums.CinemaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CinemaRequest(
        @NotBlank(message = "Vui lòng nhập tên rạp") @Size(max = 100, message = "Tên rạp tối đa 100 ký tự")
                String name,
        @NotBlank(message = "Vui lòng nhập địa chỉ") @Size(max = 255) String address,
        @NotBlank(message = "Vui lòng nhập thành phố") @Size(max = 50) String city,
        @Size(max = 20) String phone,
        @Size(max = 2000) String description,
        @Size(max = 500) String imageUrl,
        @Size(max = 20, message = "Tối đa 20 tiện ích") List<@NotBlank @Size(max = 50) String> amenities,
        @NotNull(message = "Vui lòng chọn trạng thái") CinemaStatus status) {}
