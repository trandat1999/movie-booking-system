package com.moviebooking.modules.review.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.movie.service.MovieService;
import com.moviebooking.modules.review.dto.request.ReviewRequest;
import com.moviebooking.modules.review.dto.response.MovieReviewsResponse;
import com.moviebooking.modules.review.dto.response.MyReviewResponse;
import com.moviebooking.modules.review.dto.response.ReviewResponse;
import com.moviebooking.modules.review.entity.MovieReview;
import com.moviebooking.modules.review.repository.MovieReviewRepository;
import com.moviebooking.modules.review.service.ReviewService;
import com.moviebooking.modules.user.dto.response.UserBrief;
import com.moviebooking.modules.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MovieReviewRepository reviewRepository;
    private final MovieService movieService;
    private final OrderService orderService;
    private final UserService userService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public MovieReviewsResponse forMovie(String slug, int page, int size) {
        Long movieId = movieService.getPublicBySlug(slug).id();
        long[] counts = new long[5];
        for (Object[] row : reviewRepository.countByRating(movieId)) {
            counts[((Number) row[0]).intValue() - 1] = ((Number) row[1]).longValue();
        }
        long total = Arrays.stream(counts).sum();
        long points = 0;
        for (int i = 0; i < counts.length; i++) {
            points += (i + 1) * counts[i];
        }
        double average = total == 0 ? 0 : Math.round(points * 10.0 / total) / 10.0;
        Page<MovieReview> reviews = reviewRepository.findByMovieIdAndHiddenFalse(movieId, pageable(page, size));
        return new MovieReviewsResponse(
                average, total, Arrays.stream(counts).boxed().toList(), PageResponse.from(withNames(reviews)));
    }

    @Override
    @Transactional(readOnly = true)
    public MyReviewResponse mine(Long userId, Long movieId) {
        movieService.getEntity(movieId);
        ReviewResponse review = reviewRepository.findByMovieIdAndUserId(movieId, userId)
                .map(r -> toResponse(r, userService.briefs(List.of(userId)), titles(List.of(movieId))))
                .orElse(null);
        return new MyReviewResponse(orderService.hasWatched(userId, movieId), review);
    }

    @Override
    @Transactional
    public ReviewResponse save(Long userId, Long movieId, ReviewRequest request) {
        movieService.getEntity(movieId);
        if (!orderService.hasWatched(userId, movieId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        Instant now = clock.instant();
        MovieReview review = reviewRepository.findByMovieIdAndUserId(movieId, userId).orElseGet(() -> {
            MovieReview created = new MovieReview();
            created.setMovieId(movieId);
            created.setUserId(userId);
            created.setCreatedAt(now);
            return created;
        });
        review.setRating(request.rating());
        String comment = request.comment() == null ? null : request.comment().strip();
        review.setComment(comment == null || comment.isEmpty() ? null : comment);
        review.setUpdatedAt(now);
        reviewRepository.save(review);
        return toResponse(review, userService.briefs(List.of(userId)), titles(List.of(movieId)));
    }

    @Override
    @Transactional
    public void deleteMine(Long userId, Long movieId) {
        MovieReview review = reviewRepository.findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> search(Boolean hidden, int page, int size) {
        Page<MovieReview> reviews = hidden == null
                ? reviewRepository.findAll(pageable(page, size))
                : reviewRepository.findByHidden(hidden, pageable(page, size));
        return PageResponse.from(withNames(reviews));
    }

    @Override
    @Transactional
    public ReviewResponse setHidden(Long reviewId, boolean hidden) {
        MovieReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        review.setHidden(hidden);
        return toResponse(review, userService.briefs(List.of(review.getUserId())), titles(List.of(review.getMovieId())));
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    private Page<ReviewResponse> withNames(Page<MovieReview> reviews) {
        List<Long> userIds = new ArrayList<>(reviews.map(MovieReview::getUserId).getContent());
        Map<Long, UserBrief> users = userService.briefs(userIds);
        Map<Long, String> titles = titles(reviews.map(MovieReview::getMovieId).getContent());
        return reviews.map(r -> toResponse(r, users, titles));
    }

    /** Tên phim theo id (mỗi trang tối đa vài chục phim khác nhau). */
    private Map<Long, String> titles(Collection<Long> movieIds) {
        Map<Long, String> titles = new HashMap<>();
        new HashSet<>(movieIds).forEach(id -> titles.put(id, movieService.getEntity(id).getTitle()));
        return titles;
    }

    private static ReviewResponse toResponse(MovieReview r, Map<Long, UserBrief> users, Map<Long, String> titles) {
        UserBrief user = users.get(r.getUserId());
        return new ReviewResponse(
                r.getId(),
                r.getMovieId(),
                titles.get(r.getMovieId()),
                r.getUserId(),
                user == null ? "Khán giả" : user.fullName(),
                r.getRating(),
                r.getComment(),
                r.isHidden(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }
}
