package com.moviebooking.modules.cinema.dto.request;

import com.moviebooking.modules.cinema.enums.RoomStatus;
import com.moviebooking.modules.cinema.enums.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        @NotBlank(message = "Vui lòng nhập tên phòng") @Size(max = 50, message = "Tên phòng tối đa 50 ký tự")
                String name,
        @NotNull(message = "Vui lòng chọn loại phòng") RoomType roomType,
        @NotNull(message = "Vui lòng chọn trạng thái") RoomStatus status) {}
