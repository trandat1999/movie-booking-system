package com.moviebooking.modules.cinema.repository;

import com.moviebooking.modules.cinema.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByCinemaIdOrderByNameAsc(Long cinemaId);

    @EntityGraph(attributePaths = "cinema")
    Optional<Room> findWithCinemaById(Long id);

    /** Khóa dòng phòng khi vẽ lại sơ đồ để không chạy song song với việc tạo suất chiếu đầu tiên. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findForUpdate(@Param("id") Long id);

    boolean existsByCinemaIdAndNameIgnoreCase(Long cinemaId, String name);

    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNot(Long cinemaId, String name, Long id);

    @Query("select r.cinema.id, count(r) from Room r group by r.cinema.id")
    List<Object[]> countByCinema();
}
