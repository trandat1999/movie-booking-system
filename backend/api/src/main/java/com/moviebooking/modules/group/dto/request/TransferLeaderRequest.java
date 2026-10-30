package com.moviebooking.modules.group.dto.request;

import jakarta.validation.constraints.NotNull;

public record TransferLeaderRequest(@NotNull Long userId) {}
