package com.moviebooking.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Mã lỗi dùng chung. Mã lỗi nghiệp vụ của từng module (ví dụ {@code SEAT_UNAVAILABLE}) được bổ sung vào enum
 * này khi triển khai module đó, để frontend có một danh sách mã lỗi duy nhất.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Chung
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để tiếp tục"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ"),
    CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã bị thay đổi, vui lòng thử lại"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh, vui lòng thử lại sau"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã có lỗi xảy ra, vui lòng thử lại sau"),

    // Xác thực & tài khoản (M1)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email/số điện thoại hoặc mật khẩu không đúng"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email đã được sử dụng"),
    PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng"),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    ROLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "Vai trò không tồn tại"),
    CANNOT_MODIFY_OWN_ACCOUNT(HttpStatus.BAD_REQUEST, "Không thể tự khóa hoặc đổi vai trò của chính mình"),

    // Danh mục phim (M2)
    GENRE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thể loại"),
    GENRE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Thể loại đã tồn tại"),
    GENRE_IN_USE(HttpStatus.CONFLICT, "Thể loại đang được gán cho phim, không thể xóa"),
    PERSON_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đạo diễn/diễn viên"),
    PERSON_IN_USE(HttpStatus.CONFLICT, "Người này đang gắn với phim, không thể xóa"),
    MOVIE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phim"),
    MOVIE_IN_USE(HttpStatus.CONFLICT, "Phim đã có suất chiếu, hãy chuyển trạng thái sang Ngừng chiếu thay vì xóa"),

    // Rạp & phòng chiếu (M2)
    CINEMA_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy rạp"),
    CINEMA_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tên rạp đã tồn tại"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy phòng chiếu"),
    ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "Tên phòng đã tồn tại trong rạp"),
    ROOM_LAYOUT_LOCKED(HttpStatus.CONFLICT, "Phòng đã có suất chiếu, không thể vẽ lại sơ đồ (chỉ được bật/tắt từng ghế)"),
    INVALID_SEAT_LAYOUT(HttpStatus.BAD_REQUEST, "Sơ đồ ghế không hợp lệ"),
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy ghế"),
    SEAT_TYPE_NOT_FOUND(HttpStatus.BAD_REQUEST, "Loại ghế không tồn tại"),

    // Lịch chiếu & giá (M2)
    SHOWTIME_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy suất chiếu"),
    SHOWTIME_OVERLAP(HttpStatus.CONFLICT, "Phòng đã có suất chiếu trùng thời gian (tính cả thời gian dọn phòng)"),
    SHOWTIME_IN_PAST(HttpStatus.BAD_REQUEST, "Giờ chiếu phải ở tương lai"),
    SHOWTIME_NOT_EDITABLE(HttpStatus.CONFLICT, "Suất chiếu đã bắt đầu, đã hủy hoặc đã có ghế được giữ/bán"),
    MOVIE_NOT_SCHEDULABLE(HttpStatus.BAD_REQUEST, "Phim đã ngừng chiếu, không thể tạo suất mới"),
    ROOM_NOT_SCHEDULABLE(HttpStatus.BAD_REQUEST, "Phòng đang ngừng hoạt động hoặc chưa có sơ đồ ghế"),
    HOLIDAY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ngày lễ đã tồn tại"),
    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy ngày lễ"),

    // Cấu hình (M2)
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tham số cấu hình"),
    INVALID_SETTING_VALUE(HttpStatus.BAD_REQUEST, "Giá trị cấu hình không hợp lệ");

    private final HttpStatus status;
    private final String defaultMessage;
}
