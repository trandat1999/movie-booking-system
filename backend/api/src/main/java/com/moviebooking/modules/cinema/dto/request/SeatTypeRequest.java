package com.moviebooking.modules.cinema.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Sửa tên hiển thị, phụ thu và màu của loại ghế (CIN05). Sức chứa và số ô là cố định. */
public record SeatTypeRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(value = 0, message = "Phụ thu không âm") @Max(value = 10_000_000) Long surcharge,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Màu dạng #RRGGBB") String color) {}
