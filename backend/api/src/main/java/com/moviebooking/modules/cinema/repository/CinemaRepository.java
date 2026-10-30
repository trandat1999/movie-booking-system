package com.moviebooking.modules.cinema.repository;

import com.moviebooking.modules.cinema.entity.Cinema;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {

    List<Cinema> findAllByOrderByCityAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
