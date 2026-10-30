package com.moviebooking.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Chi tiết lỗi trả về cho client.
 *
 * @param code mã lỗi ổn định để frontend xử lý (ví dụ {@code SEAT_UNAVAILABLE})
 * @param message thông điệp tiếng Việt hiển thị cho người dùng
 * @param details dữ liệu bổ sung (danh sách lỗi theo trường, danh sách ghế bị mất, ...)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, Object details) {}
