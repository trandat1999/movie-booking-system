package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.TicketStatus;
import java.time.Instant;
import java.util.List;

/**
 * Kết quả soát vé (CHK01).
 *
 * @param checkedIn số vé vừa được soát trong lần quét này
 */
public record CheckinResponse(
        String orderCode, BookingShowtimeInfo showtime, int checkedIn, List<CheckinTicket> tickets) {

    public record CheckinTicket(String code, String seatLabel, String holderName, TicketStatus status, Instant checkedInAt) {}
}
