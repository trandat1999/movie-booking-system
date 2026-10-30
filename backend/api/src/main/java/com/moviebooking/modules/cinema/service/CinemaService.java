package com.moviebooking.modules.cinema.service;

import com.moviebooking.modules.cinema.dto.request.CinemaRequest;
import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import java.util.List;

/** Quản lý rạp (CIN01, CIN02, CIN09). */
public interface CinemaService {

    List<CinemaResponse> list();

    CinemaResponse get(Long id);

    CinemaResponse create(CinemaRequest request);

    CinemaResponse update(Long id, CinemaRequest request);
}
