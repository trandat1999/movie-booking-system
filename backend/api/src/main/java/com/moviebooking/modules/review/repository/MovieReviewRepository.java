package com.moviebooking.modules.review.repository;

import com.moviebooking.modules.review.entity.MovieReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieReviewRepository extends JpaRepository<MovieReview, Long> {

    Page<MovieReview> findByMovieIdAndHiddenFalse(Long movieId, Pageable pageable);

    Optional<MovieReview> findByMovieIdAndUserId(Long movieId, Long userId);

    Page<MovieReview> findByHidden(boolean hidden, Pageable pageable);

    /** Số đánh giá hiển thị theo từng mức sao: mỗi phần tử {@code [rating, count]}. */
    @Query("select r.rating, count(r) from MovieReview r where r.movieId = :movieId and r.hidden = false "
            + "group by r.rating")
    List<Object[]> countByRating(@Param("movieId") Long movieId);
}
