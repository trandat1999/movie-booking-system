package com.moviebooking.modules.cinema.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Sơ đồ phòng dạng lưới (CIN04). Chỉ gửi vị trí và loại ghế; nhãn hàng/số ghế do backend tính.
 * Chỉ số hàng/cột bắt đầu từ 0, hàng 0 gần màn hình nhất.
 */
public record RoomLayoutRequest(
        @NotNull @Min(value = 1, message = "Số hàng tối thiểu 1") @Max(value = 30, message = "Số hàng tối đa 30")
                Integer gridRows,
        @NotNull @Min(value = 1, message = "Số cột tối thiểu 1") @Max(value = 40, message = "Số cột tối đa 40")
                Integer gridCols,
        @NotEmpty(message = "Sơ đồ phải có ít nhất một ghế") @Size(max = 1200) List<@Valid SeatCell> seats) {

    public record SeatCell(@NotNull Integer gridRow, @NotNull Integer gridCol, @NotBlank String seatTypeCode) {}
}
