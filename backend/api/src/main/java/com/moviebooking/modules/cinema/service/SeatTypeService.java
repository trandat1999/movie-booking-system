package com.moviebooking.modules.cinema.service;

import com.moviebooking.modules.cinema.dto.request.SeatTypeRequest;
import com.moviebooking.modules.cinema.dto.response.SeatTypeResponse;
import com.moviebooking.modules.cinema.entity.SeatType;
import java.util.List;
import java.util.Map;

/** Loại ghế và phụ thu (CIN05). */
public interface SeatTypeService {

    List<SeatTypeResponse> list();

    SeatTypeResponse update(String code, SeatTypeRequest request);

    /** code → loại ghế, dùng để tính giá. */
    Map<String, SeatType> getAll();
}
