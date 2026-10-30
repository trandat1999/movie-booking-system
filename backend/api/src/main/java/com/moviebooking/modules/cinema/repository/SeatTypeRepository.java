package com.moviebooking.modules.cinema.repository;

import com.moviebooking.modules.cinema.entity.SeatType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatTypeRepository extends JpaRepository<SeatType, String> {

    List<SeatType> findAllByOrderBySortOrderAsc();
}
