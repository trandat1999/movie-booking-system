package com.moviebooking.modules.cinema.service;

import com.moviebooking.modules.cinema.dto.request.RoomLayoutRequest;
import com.moviebooking.modules.cinema.dto.request.RoomRequest;
import com.moviebooking.modules.cinema.dto.response.RoomLayoutResponse;
import com.moviebooking.modules.cinema.dto.response.RoomResponse;
import com.moviebooking.modules.cinema.dto.response.SeatResponse;
import com.moviebooking.modules.cinema.entity.Room;
import com.moviebooking.modules.cinema.entity.Seat;
import java.util.List;

/** Phòng chiếu và sơ đồ ghế (CIN03–CIN08). */
public interface RoomService {

    List<RoomResponse> listByCinema(Long cinemaId);

    RoomLayoutResponse getLayout(Long roomId);

    RoomResponse create(Long cinemaId, RoomRequest request);

    RoomResponse update(Long roomId, RoomRequest request);

    /**
     * Vẽ lại toàn bộ sơ đồ. Bị từ chối khi phòng đã có suất chiếu ({@code layoutLocked}) vì mã ghế phải ổn định.
     */
    RoomLayoutResponse saveLayout(Long roomId, RoomLayoutRequest request);

    /** Bật/tắt bán một ghế; được phép cả khi sơ đồ đã khóa. */
    SeatResponse setSeatActive(Long roomId, Long seatId, boolean active);

    // --- Dùng cho module lịch chiếu ---

    /** Phòng kèm rạp. */
    Room getEntity(Long roomId);

    List<Seat> listSeats(Long roomId);

    /**
     * Khóa sơ đồ khi tạo suất chiếu; khóa dòng phòng (SELECT ... FOR UPDATE) để không chạy song song với việc vẽ
     * lại sơ đồ.
     */
    void lockLayout(Long roomId);
}
