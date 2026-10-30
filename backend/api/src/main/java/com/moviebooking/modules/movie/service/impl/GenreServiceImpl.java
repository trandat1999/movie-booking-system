package com.moviebooking.modules.movie.service.impl;

import com.moviebooking.common.util.Slugs;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.movie.dto.request.GenreRequest;
import com.moviebooking.modules.movie.dto.response.GenreResponse;
import com.moviebooking.modules.movie.entity.Genre;
import com.moviebooking.modules.movie.mapper.MovieMapper;
import com.moviebooking.modules.movie.repository.GenreRepository;
import com.moviebooking.modules.movie.service.GenreService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final MovieMapper movieMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponse> list() {
        return genreRepository.findAllByOrderByNameAsc().stream()
                .map(movieMapper::toGenre)
                .toList();
    }

    @Override
    @Transactional
    public GenreResponse create(GenreRequest request) {
        String name = request.name().trim();
        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.GENRE_ALREADY_EXISTS);
        }
        Genre genre = new Genre();
        apply(genre, name);
        return movieMapper.toGenre(genreRepository.save(genre));
    }

    @Override
    @Transactional
    public GenreResponse update(Long id, GenreRequest request) {
        Genre genre = find(id);
        String name = request.name().trim();
        if (genreRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.GENRE_ALREADY_EXISTS);
        }
        apply(genre, name);
        return movieMapper.toGenre(genre);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Genre genre = find(id);
        try {
            genreRepository.delete(genre);
            genreRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.GENRE_IN_USE);
        }
    }

    private void apply(Genre genre, String name) {
        genre.setName(name);
        String slug = Slugs.of(name);
        if (!slug.equals(genre.getSlug()) && genreRepository.existsBySlug(slug)) {
            throw new BusinessException(ErrorCode.GENRE_ALREADY_EXISTS);
        }
        genre.setSlug(slug);
    }

    private Genre find(Long id) {
        return genreRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.GENRE_NOT_FOUND));
    }
}
