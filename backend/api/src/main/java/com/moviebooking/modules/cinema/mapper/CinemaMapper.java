package com.moviebooking.modules.cinema.mapper;

import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import com.moviebooking.modules.cinema.dto.response.RoomResponse;
import com.moviebooking.modules.cinema.dto.response.SeatResponse;
import com.moviebooking.modules.cinema.dto.response.SeatTypeResponse;
import com.moviebooking.modules.cinema.entity.Cinema;
import com.moviebooking.modules.cinema.entity.Room;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.entity.SeatType;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper
public interface CinemaMapper {

    SeatResponse toSeat(Seat seat);

    SeatTypeResponse toSeatType(SeatType seatType);

    default CinemaResponse toCinema(Cinema cinema, long roomCount) {
        return new CinemaResponse(
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                cinema.getCity(),
                cinema.getPhone(),
                cinema.getDescription(),
                cinema.getImageUrl(),
                List.copyOf(cinema.getAmenities()),
                cinema.getStatus(),
                roomCount);
    }

    default RoomResponse toRoom(Room room, long seatCount, long capacity) {
        return new RoomResponse(
                room.getId(),
                room.getCinema().getId(),
                room.getCinema().getName(),
                room.getName(),
                room.getRoomType(),
                room.getStatus(),
                room.getGridRows(),
                room.getGridCols(),
                room.isLayoutLocked(),
                seatCount,
                capacity);
    }
}
