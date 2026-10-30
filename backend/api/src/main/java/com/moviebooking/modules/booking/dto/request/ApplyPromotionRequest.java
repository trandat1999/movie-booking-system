package com.moviebooking.modules.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyPromotionRequest(@NotBlank @Size(max = 30) String code) {}
