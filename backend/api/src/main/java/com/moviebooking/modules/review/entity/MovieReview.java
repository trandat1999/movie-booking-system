package com.moviebooking.modules.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** Đánh giá phim của người đã xem (FBK01). Mỗi người một đánh giá cho mỗi phim. */
@Getter
@Setter
@Entity
@Table(name = "movie_reviews")
public class MovieReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 1–5 sao. */
    @Column(nullable = false)
    private int rating;

    private String comment;

    /** Quản trị ẩn đánh giá vi phạm; không tính vào điểm trung bình. */
    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
