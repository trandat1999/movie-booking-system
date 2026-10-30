package com.moviebooking.modules.booking.service;

import com.moviebooking.modules.booking.dto.response.CheckinResponse;

/** Soát vé tại rạp (CHK01): quét QR vé (một người) hoặc QR đơn (cả nhóm). Mỗi vé chỉ soát được một lần. */
public interface CheckinService {

    /** Mở soát vé trước giờ chiếu (phút). */
    int OPENS_BEFORE_MIN = 120;

    /**
     * @param payload nội dung QR đã ký (BR07)
     * @throws com.moviebooking.exception.BusinessException INVALID_TICKET_QR, TICKET_ALREADY_USED, TICKET_NOT_VALID,
     *     CHECKIN_NOT_OPEN
     */
    CheckinResponse checkin(Long staffId, String payload);
}
