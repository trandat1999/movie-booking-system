package com.moviebooking.modules.movie.repository;

import com.moviebooking.modules.movie.entity.Movie;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    @EntityGraph(attributePaths = {"genres", "credits", "credits.person"})
    Optional<Movie> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"genres", "credits", "credits.person"})
    Optional<Movie> findWithDetailsBySlug(String slug);

    /** Ngôn ngữ của các phim khách hàng thấy được (đang chiếu, sắp chiếu) — dùng cho bộ lọc. */
    @Query("select distinct m.language from Movie m "
            + "where m.status <> com.moviebooking.modules.movie.enums.MovieStatus.ENDED order by m.language")
    List<String> findVisibleLanguages();

    /**
     * Phim tương tự (MOV05): đang/sắp chiếu, có chung thể loại với phim đang xem; nhiều thể loại chung hơn rồi mới
     * hơn xếp trước.
     */
    @Query("select m.id from Movie m join m.genres g "
            + "where g.id in :genreIds and m.id <> :movieId "
            + "and m.status <> com.moviebooking.modules.movie.enums.MovieStatus.ENDED "
            + "group by m.id, m.releaseDate order by count(g) desc, m.releaseDate desc, m.id desc")
    List<Long> findSimilarIds(Collection<Long> genreIds, Long movieId, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
