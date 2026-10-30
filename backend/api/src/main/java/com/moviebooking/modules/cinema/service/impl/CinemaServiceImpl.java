package com.moviebooking.modules.cinema.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.cinema.dto.request.CinemaRequest;
import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import com.moviebooking.modules.cinema.entity.Cinema;
import com.moviebooking.modules.cinema.enums.CinemaStatus;
import com.moviebooking.modules.cinema.mapper.CinemaMapper;
import com.moviebooking.modules.cinema.repository.CinemaRepository;
import com.moviebooking.modules.cinema.repository.RoomRepository;
import com.moviebooking.modules.cinema.service.CinemaService;
import com.moviebooking.common.util.Strings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final CinemaMapper cinemaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> list() {
        Map<Long, Long> roomCounts = new HashMap<>();
        roomRepository.countByCinema().forEach(row -> roomCounts.put((Long) row[0], (Long) row[1]));
        return cinemaRepository.findAllByOrderByCityAscNameAsc().stream()
                .map(cinema -> cinemaMapper.toCinema(cinema, roomCounts.getOrDefault(cinema.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public CinemaResponse create(CinemaRequest request) {
        if (cinemaRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessException(ErrorCode.CINEMA_ALREADY_EXISTS);
        }
        Cinema cinema = new Cinema();
        apply(cinema, request);
        return cinemaMapper.toCinema(cinemaRepository.save(cinema), 0);
    }

    @Override
    @Transactional
    public CinemaResponse update(Long id, CinemaRequest request) {
        Cinema cinema = find(id);
        if (cinemaRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new BusinessException(ErrorCode.CINEMA_ALREADY_EXISTS);
        }
        apply(cinema, request);
        return toResponse(cinema);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> listPublic() {
        return list().stream().filter(c -> c.status() == CinemaStatus.ACTIVE).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getPublic(Long id) {
        Cinema cinema = find(id);
        if (cinema.getStatus() != CinemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        return toResponse(cinema);
    }

    private CinemaResponse toResponse(Cinema cinema) {
        return cinemaMapper.toCinema(cinema, roomRepository.findByCinemaIdOrderByNameAsc(cinema.getId()).size());
    }

    private static void apply(Cinema cinema, CinemaRequest request) {
        cinema.setName(request.name().trim());
        cinema.setAddress(request.address().trim());
        cinema.setCity(request.city().trim());
        cinema.setPhone(Strings.blankToNull(request.phone()));
        cinema.setDescription(Strings.blankToNull(request.description()));
        cinema.setImageUrl(Strings.blankToNull(request.imageUrl()));
        cinema.setAmenities(request.amenities() == null
                ? List.of()
                : request.amenities().stream().map(String::trim).distinct().toList());
        cinema.setStatus(request.status());
    }

    @Override
    @Transactional(readOnly = true)
    public Cinema getEntity(Long id) {
        return find(id);
    }

    private Cinema find(Long id) {
        return cinemaRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));
    }
}
