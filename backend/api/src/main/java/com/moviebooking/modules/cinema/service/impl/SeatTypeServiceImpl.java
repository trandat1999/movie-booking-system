package com.moviebooking.modules.cinema.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.cinema.dto.request.SeatTypeRequest;
import com.moviebooking.modules.cinema.dto.response.SeatTypeResponse;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.mapper.CinemaMapper;
import com.moviebooking.modules.cinema.repository.SeatTypeRepository;
import com.moviebooking.modules.cinema.service.SeatTypeService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SeatTypeServiceImpl implements SeatTypeService {

    private final SeatTypeRepository seatTypeRepository;
    private final CinemaMapper cinemaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SeatTypeResponse> list() {
        return seatTypeRepository.findAllByOrderBySortOrderAsc().stream()
                .map(cinemaMapper::toSeatType)
                .toList();
    }

    @Override
    @Transactional
    public SeatTypeResponse update(String code, SeatTypeRequest request) {
        SeatType seatType = seatTypeRepository
                .findById(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND));
        seatType.setName(request.name().trim());
        seatType.setSurcharge(request.surcharge());
        seatType.setColor(request.color().toLowerCase());
        return cinemaMapper.toSeatType(seatType);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, SeatType> getAll() {
        Map<String, SeatType> result = new LinkedHashMap<>();
        seatTypeRepository.findAllByOrderBySortOrderAsc().forEach(t -> result.put(t.getCode(), t));
        return result;
    }
}
