package com.moviebooking.modules.movie.repository;

import com.moviebooking.modules.movie.entity.Genre;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import jakarta.persistence.criteria.Join;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class MovieSpecifications {

    private MovieSpecifications() {}

    /** Tìm theo tên phim (tiếng Việt hoặc tên gốc). */
    public static Specification<Movie> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("originalTitle")), pattern));
    }

    public static Specification<Movie> hasStatus(MovieStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Phim khách hàng được thấy: đang chiếu hoặc sắp chiếu. */
    public static Specification<Movie> visibleToPublic() {
        return (root, query, cb) -> root.get("status").in(MovieStatus.NOW_SHOWING, MovieStatus.COMING_SOON);
    }

    public static Specification<Movie> hasAgeRating(AgeRating ageRating) {
        return ageRating == null ? null : (root, query, cb) -> cb.equal(root.get("ageRating"), ageRating);
    }

    public static Specification<Movie> hasLanguage(String language) {
        return language == null || language.isBlank()
                ? null
                : (root, query, cb) -> cb.equal(root.get("language"), language);
    }

    public static Specification<Movie> hasGenre(Long genreId) {
        if (genreId == null) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Movie, Genre> genres = root.join("genres");
            return cb.equal(genres.get("id"), genreId);
        };
    }
}
