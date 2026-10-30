package com.moviebooking.modules.cinema.service;

import com.moviebooking.modules.cinema.dto.request.CinemaRequest;
import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import com.moviebooking.modules.cinema.entity.Cinema;
import java.util.List;

/** Quản lý rạp (CIN01, CIN02, CIN09). */
public interface CinemaService {

    List<CinemaResponse> list();

    CinemaResponse get(Long id);

    CinemaResponse create(CinemaRequest request);

    CinemaResponse update(Long id, CinemaRequest request);

    /** Khách hàng: chỉ rạp đang hoạt động (SCH01). */
    List<CinemaResponse> listPublic();

    /** Khách hàng: rạp ngừng hoạt động trả về CINEMA_NOT_FOUND (SCH02). */
    CinemaResponse getPublic(Long id);

    /** Dùng cho module khác (nhóm): rạp theo id, kể cả đã ngừng hoạt động. */
    Cinema getEntity(Long id);
}
