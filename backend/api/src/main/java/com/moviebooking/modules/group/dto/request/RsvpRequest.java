package com.moviebooking.modules.group.dto.request;

import com.moviebooking.modules.group.enums.Rsvp;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** @param maxBudget ngân sách tối đa mỗi người (VND), null = không giới hạn */
public record RsvpRequest(@NotNull Rsvp rsvp, @Min(0) @Max(20) int guestCount, @Positive Long maxBudget) {}
