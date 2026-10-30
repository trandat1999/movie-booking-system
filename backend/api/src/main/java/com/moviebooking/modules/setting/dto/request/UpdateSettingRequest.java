package com.moviebooking.modules.setting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSettingRequest(@NotBlank(message = "Vui lòng nhập giá trị") @Size(max = 500) String value) {}
