package com.moviebooking.modules.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phim yêu thích của người dùng (MOV04). */
@Getter
@Setter
@Entity
@Table(name = "favorite_movies")
@IdClass(FavoriteMovie.Key.class)
public class FavoriteMovie {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {
        private Long userId;
        private Long movieId;

        public Key(Long userId, Long movieId) {
            this.userId = userId;
            this.movieId = movieId;
        }
    }
}
