package com.moviebooking.modules.movie.repository;

import com.moviebooking.modules.movie.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Page<Person> findByFullNameContainingIgnoreCase(String keyword, Pageable pageable);
}
