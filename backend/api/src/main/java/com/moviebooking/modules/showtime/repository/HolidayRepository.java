package com.moviebooking.modules.showtime.repository;

import com.moviebooking.modules.showtime.entity.Holiday;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, LocalDate> {

    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
