package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SeatSelectionRequest(@NotEmpty @Size(max = 50) List<@NotNull Long> seatIds) {}
