package com.moviebooking.modules.movie.repository;

import com.moviebooking.modules.movie.entity.FavoriteMovie;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteMovieRepository extends JpaRepository<FavoriteMovie, FavoriteMovie.Key> {

    List<FavoriteMovie> findByUserIdOrderByCreatedAtDesc(Long userId);
}
