package com.moviebooking.modules.cinema.repository;

import com.moviebooking.modules.cinema.entity.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoomIdOrderByGridRowAscGridColAsc(Long roomId);

    Optional<Seat> findByIdAndRoomId(Long id, Long roomId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Seat s where s.room.id = :roomId")
    int deleteByRoomId(@Param("roomId") Long roomId);

    /** [roomId, số ghế, tổng chỗ ngồi] */
    @Query("select s.room.id, count(s), sum(t.capacity) from Seat s, SeatType t "
            + "where t.code = s.seatTypeCode and s.room.id in :roomIds group by s.room.id")
    List<Object[]> countByRooms(@Param("roomIds") List<Long> roomIds);
}
