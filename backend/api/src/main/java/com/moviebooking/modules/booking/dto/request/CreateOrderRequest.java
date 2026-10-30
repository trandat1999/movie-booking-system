package com.moviebooking.modules.booking.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotNull Long holdId) {}
