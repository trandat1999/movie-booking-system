package com.moviebooking.modules.group.dto.request;

import com.moviebooking.modules.group.enums.SeatPreference;
import jakarta.validation.constraints.NotNull;

/**
 * Sở thích chỗ ngồi (GRP22).
 *
 * @param nextToUserId thành viên muốn ngồi cạnh; {@code null} = không yêu cầu
 */
public record SeatPreferenceRequest(@NotNull SeatPreference preference, Long nextToUserId) {}
