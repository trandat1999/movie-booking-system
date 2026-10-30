package com.moviebooking.modules.movie.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.util.Slugs;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.movie.dto.request.MovieRequest;
import com.moviebooking.modules.movie.dto.response.MovieDetailResponse;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.entity.Genre;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.entity.Person;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.mapper.MovieMapper;
import com.moviebooking.modules.movie.repository.GenreRepository;
import com.moviebooking.modules.movie.repository.MovieRepository;
import com.moviebooking.modules.movie.repository.MovieSpecifications;
import com.moviebooking.modules.movie.repository.PersonRepository;
import com.moviebooking.modules.movie.service.MovieService;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;
    private final MovieMapper movieMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MovieSummaryResponse> search(
            String keyword, MovieStatus status, Long genreId, int page, int size) {
        Specification<Movie> spec = Specification.allOf(
                MovieSpecifications.keyword(keyword),
                MovieSpecifications.hasStatus(status),
                MovieSpecifications.hasGenre(genreId));
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "releaseDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(movieRepository.findAll(spec, pageable).map(movieMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDetailResponse get(Long id) {
        return movieMapper.toDetail(findWithDetails(id));
    }

    @Override
    @Transactional
    public MovieDetailResponse create(MovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        return movieMapper.toDetail(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public MovieDetailResponse update(Long id, MovieRequest request) {
        Movie movie = findWithDetails(id);
        apply(movie, request);
        return movieMapper.toDetail(movie);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Movie movie = findWithDetails(id);
        try {
            movieRepository.delete(movie);
            movieRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // FK từ showtimes chặn xóa phim đã có suất chiếu
            throw new BusinessException(ErrorCode.MOVIE_IN_USE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Movie getEntity(Long id) {
        return movieRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }

    private void apply(Movie movie, MovieRequest request) {
        String title = request.title().trim();
        movie.setTitle(title);
        movie.setSlug(uniqueSlug(title, movie.getId()));
        movie.setOriginalTitle(blankToNull(request.originalTitle()));
        movie.setDescription(blankToNull(request.description()));
        movie.setDurationMin(request.durationMin());
        movie.setAgeRating(request.ageRating());
        movie.setLanguage(request.language().trim());
        movie.setReleaseDate(request.releaseDate());
        movie.setPosterUrl(blankToNull(request.posterUrl()));
        movie.setBannerUrl(blankToNull(request.bannerUrl()));
        movie.setTrailerUrl(blankToNull(request.trailerUrl()));
        movie.setStatus(request.status());

        List<Genre> genres = genreRepository.findAllById(request.genreIds());
        if (genres.size() != request.genreIds().size()) {
            throw new BusinessException(ErrorCode.GENRE_NOT_FOUND);
        }
        movie.setGenres(new HashSet<>(genres));
        movie.replaceCredits(loadPeople(request.directorIds()), loadPeople(request.actorIds()));
    }

    /** Giữ thứ tự truyền vào, bỏ id trùng. */
    private List<Person> loadPeople(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = List.copyOf(new LinkedHashSet<>(ids));
        Map<Long, Person> byId = personRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        if (byId.size() != distinct.size()) {
            throw new BusinessException(ErrorCode.PERSON_NOT_FOUND);
        }
        return distinct.stream().map(byId::get).toList();
    }

    /** Slug duy nhất: thêm hậu tố -2, -3, ... nếu trùng phim khác. */
    private String uniqueSlug(String title, Long currentId) {
        String base = Slugs.of(title);
        String slug = base;
        for (int i = 2; isTaken(slug, currentId); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private boolean isTaken(String slug, Long currentId) {
        return currentId == null ? movieRepository.existsBySlug(slug) : movieRepository.existsBySlugAndIdNot(slug, currentId);
    }

    private Movie findWithDetails(Long id) {
        return movieRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
