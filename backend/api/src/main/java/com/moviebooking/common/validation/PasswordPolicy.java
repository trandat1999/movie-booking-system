package com.moviebooking.common.validation;

/** Quy tắc mật khẩu dùng chung cho đăng ký và đổi mật khẩu. */
public final class PasswordPolicy {

    /** 8–64 ký tự, có ít nhất một chữ cái và một chữ số. */
    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";

    public static final String MESSAGE = "Mật khẩu dài 8–64 ký tự, gồm cả chữ và số";

    private PasswordPolicy() {}
}
