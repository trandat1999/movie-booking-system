package com.moviebooking.modules.auth.dto.request;

import com.moviebooking.common.util.PhoneNumbers;
import com.moviebooking.common.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Đăng ký tài khoản khách hàng (USR01). */
public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(min = 2, max = 100, message = "Họ tên dài 2–100 ký tự")
                String fullName,
        @NotBlank(message = "Vui lòng nhập email")
                @Email(message = "Email không hợp lệ")
                @Size(max = 255, message = "Email quá dài")
                String email,
        @Pattern(regexp = PhoneNumbers.VN_MOBILE_PATTERN, message = "Số điện thoại không hợp lệ") String phone,
        @NotBlank(message = "Vui lòng nhập mật khẩu")
                @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
                String password,
        @Past(message = "Ngày sinh phải ở quá khứ") LocalDate dateOfBirth) {

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + "]";
    }
}
