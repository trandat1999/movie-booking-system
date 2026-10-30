package com.moviebooking.modules.user.dto.request;

import com.moviebooking.common.util.PhoneNumbers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(min = 2, max = 100, message = "Họ tên dài 2–100 ký tự")
                String fullName,
        @Pattern(regexp = PhoneNumbers.VN_MOBILE_PATTERN, message = "Số điện thoại không hợp lệ") String phone,
        @Past(message = "Ngày sinh phải ở quá khứ") LocalDate dateOfBirth,
        @Size(max = 500, message = "Đường dẫn ảnh quá dài") String avatarUrl) {}
