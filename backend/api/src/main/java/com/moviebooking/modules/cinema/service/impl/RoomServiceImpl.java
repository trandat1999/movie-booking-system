package com.moviebooking.modules.cinema.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.cinema.dto.request.RoomLayoutRequest;
import com.moviebooking.modules.cinema.dto.request.RoomRequest;
import com.moviebooking.modules.cinema.dto.response.RoomLayoutResponse;
import com.moviebooking.modules.cinema.dto.response.RoomResponse;
import com.moviebooking.modules.cinema.dto.response.SeatResponse;
import com.moviebooking.modules.cinema.entity.Cinema;
import com.moviebooking.modules.cinema.entity.Room;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.event.SeatAvailabilityChangedEvent;
import com.moviebooking.modules.cinema.mapper.CinemaMapper;
import com.moviebooking.modules.cinema.repository.CinemaRepository;
import com.moviebooking.modules.cinema.repository.RoomRepository;
import com.moviebooking.modules.cinema.repository.SeatRepository;
import com.moviebooking.modules.cinema.repository.SeatTypeRepository;
import com.moviebooking.modules.cinema.service.RoomService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final CinemaMapper cinemaMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> listByCinema(Long cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        List<Room> rooms = roomRepository.findByCinemaIdOrderByNameAsc(cinemaId);
        Map<Long, long[]> counts = new HashMap<>();
        if (!rooms.isEmpty()) {
            seatRepository
                    .countByRooms(rooms.stream().map(Room::getId).toList())
                    .forEach(row -> counts.put((Long) row[0], new long[] {(Long) row[1], (Long) row[2]}));
        }
        return rooms.stream()
                .map(room -> {
                    long[] c = counts.getOrDefault(room.getId(), new long[] {0, 0});
                    return cinemaMapper.toRoom(room, c[0], c[1]);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomLayoutResponse getLayout(Long roomId) {
        return toLayout(getEntity(roomId));
    }

    @Override
    @Transactional
    public RoomResponse create(Long cinemaId, RoomRequest request) {
        Cinema cinema = cinemaRepository
                .findById(cinemaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));
        if (roomRepository.existsByCinemaIdAndNameIgnoreCase(cinemaId, request.name().trim())) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_EXISTS);
        }
        Room room = new Room();
        room.setCinema(cinema);
        apply(room, request);
        return cinemaMapper.toRoom(roomRepository.save(room), 0, 0);
    }

    @Override
    @Transactional
    public RoomResponse update(Long roomId, RoomRequest request) {
        Room room = getEntity(roomId);
        if (roomRepository.existsByCinemaIdAndNameIgnoreCaseAndIdNot(
                room.getCinema().getId(), request.name().trim(), roomId)) {
            throw new BusinessException(ErrorCode.ROOM_ALREADY_EXISTS);
        }
        apply(room, request);
        return toLayout(room).room();
    }

    @Override
    @Transactional
    public RoomLayoutResponse saveLayout(Long roomId, RoomLayoutRequest request) {
        Room room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        if (room.isLayoutLocked()) {
            throw new BusinessException(ErrorCode.ROOM_LAYOUT_LOCKED);
        }
        Map<String, Integer> spanByType = seatTypeRepository.findAll().stream()
                .collect(Collectors.toMap(SeatType::getCode, SeatType::getColSpan));
        List<SeatLayoutPlanner.PlannedSeat> planned;
        try {
            planned = SeatLayoutPlanner.plan(
                    request.gridRows(),
                    request.gridCols(),
                    request.seats().stream()
                            .map(c -> new SeatLayoutPlanner.Cell(c.gridRow(), c.gridCol(), c.seatTypeCode()))
                            .toList(),
                    spanByType);
        } catch (SeatLayoutPlanner.InvalidLayoutException ex) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_LAYOUT, ErrorCode.INVALID_SEAT_LAYOUT.getDefaultMessage(), ex.errors());
        }

        // Xóa hết rồi chèn lại (xóa trước bằng câu lệnh riêng để không vướng ràng buộc UNIQUE theo ô lưới)
        seatRepository.deleteByRoomId(roomId);
        room = roomRepository.findWithCinemaById(roomId).orElseThrow();
        room.setGridRows(request.gridRows());
        room.setGridCols(request.gridCols());
        for (SeatLayoutPlanner.PlannedSeat p : planned) {
            Seat seat = new Seat();
            seat.setRoom(room);
            seat.setRowLabel(p.rowLabel());
            seat.setNumber(p.number());
            seat.setGridRow(p.gridRow());
            seat.setGridCol(p.gridCol());
            seat.setColSpan(p.colSpan());
            seat.setSeatTypeCode(p.seatTypeCode());
            seatRepository.save(seat);
        }
        return toLayout(room);
    }

    @Override
    @Transactional
    public SeatResponse setSeatActive(Long roomId, Long seatId, boolean active) {
        Seat seat = seatRepository
                .findByIdAndRoomId(seatId, roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
        if (seat.isActive() != active) {
            seat.setActive(active);
            eventPublisher.publishEvent(new SeatAvailabilityChangedEvent(roomId, seatId, active));
        }
        return cinemaMapper.toSeat(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public Room getEntity(Long roomId) {
        return roomRepository
                .findWithCinemaById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Seat> listSeats(Long roomId) {
        return seatRepository.findByRoomIdOrderByGridRowAscGridColAsc(roomId);
    }

    @Override
    @Transactional
    public void lockLayout(Long roomId) {
        Room room = roomRepository.findForUpdate(roomId).orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        room.setLayoutLocked(true);
    }

    private RoomLayoutResponse toLayout(Room room) {
        List<Seat> seats = seatRepository.findByRoomIdOrderByGridRowAscGridColAsc(room.getId());
        Map<String, SeatType> types = seatTypeRepository.findAll().stream()
                .collect(Collectors.toMap(SeatType::getCode, t -> t));
        long capacity = seats.stream().mapToLong(s -> types.get(s.getSeatTypeCode()).getCapacity()).sum();
        return new RoomLayoutResponse(
                cinemaMapper.toRoom(room, seats.size(), capacity),
                seats.stream().map(cinemaMapper::toSeat).toList(),
                seatTypeRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(cinemaMapper::toSeatType)
                        .toList());
    }

    private static void apply(Room room, RoomRequest request) {
        room.setName(request.name().trim());
        room.setRoomType(request.roomType());
        room.setStatus(request.status());
    }
}
