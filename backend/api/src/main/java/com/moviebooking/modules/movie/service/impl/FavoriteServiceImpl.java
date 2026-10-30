package com.moviebooking.modules.movie.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.entity.FavoriteMovie;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.mapper.MovieMapper;
import com.moviebooking.modules.movie.repository.FavoriteMovieRepository;
import com.moviebooking.modules.movie.repository.MovieRepository;
import com.moviebooking.modules.movie.service.FavoriteService;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMovieRepository favoriteRepository;
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<MovieSummaryResponse> list(Long userId) {
        List<Long> ids = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FavoriteMovie::getMovieId)
                .toList();
        Map<Long, Movie> movies = movieRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        return ids.stream().map(movies::get).map(movieMapper::toSummary).toList();
    }

    @Override
    @Transactional
    public void add(Long userId, Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND);
        }
        if (favoriteRepository.existsById(new FavoriteMovie.Key(userId, movieId))) {
            return;
        }
        FavoriteMovie favorite = new FavoriteMovie();
        favorite.setUserId(userId);
        favorite.setMovieId(movieId);
        favorite.setCreatedAt(clock.instant());
        favoriteRepository.save(favorite);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long movieId) {
        favoriteRepository.deleteById(new FavoriteMovie.Key(userId, movieId));
    }
}
