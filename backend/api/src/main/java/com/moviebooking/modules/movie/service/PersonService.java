package com.moviebooking.modules.movie.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.movie.dto.request.PersonRequest;
import com.moviebooking.modules.movie.dto.response.PersonResponse;

/** Quản lý đạo diễn, diễn viên (MOVADM06, MOVADM07). */
public interface PersonService {

    PageResponse<PersonResponse> search(String keyword, int page, int size);

    PersonResponse create(PersonRequest request);

    PersonResponse update(Long id, PersonRequest request);

    void delete(Long id);
}
