package com.moviebooking.modules.cinema.dto.response;

import java.util.List;

/** Phòng kèm toàn bộ ghế — dùng cho trình thiết kế sơ đồ và xem trước (CIN08). */
public record RoomLayoutResponse(RoomResponse room, List<SeatResponse> seats, List<SeatTypeResponse> seatTypes) {}
