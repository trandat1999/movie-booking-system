package com.moviebooking.modules.movie.service;

import com.moviebooking.modules.movie.dto.request.GenreRequest;
import com.moviebooking.modules.movie.dto.response.GenreResponse;
import java.util.List;

/** Quản lý thể loại phim (MOVADM05). */
public interface GenreService {

    List<GenreResponse> list();

    GenreResponse create(GenreRequest request);

    GenreResponse update(Long id, GenreRequest request);

    /** Không xóa được thể loại đang gán cho phim. */
    void delete(Long id);
}
