import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { PageResponse } from '../models/api-response.model';
import { MovieSummary } from '../models/catalog.model';
import { MovieReviews, MyReview, ReviewRequest, ReviewView } from '../models/review.model';

/** Phim yêu thích (MOV04), phim tương tự (MOV05), đánh giá phim (FBK01) — M10. */
@Injectable({ providedIn: 'root' })
export class ReviewApi {
  private readonly api = inject(ApiClient);

  similarMovies(slug: string): Observable<MovieSummary[]> {
    return this.api.get<MovieSummary[]>(`/movies/${encodeURIComponent(slug)}/similar`);
  }

  movieReviews(slug: string, page = 0, size = 10): Observable<MovieReviews> {
    return this.api.get<MovieReviews>(`/movies/${encodeURIComponent(slug)}/reviews`, { params: { page, size } });
  }

  myReview(movieId: number): Observable<MyReview> {
    return this.api.get<MyReview>(`/movies/${movieId}/my-review`);
  }

  saveReview(movieId: number, request: ReviewRequest): Observable<ReviewView> {
    return this.api.put<ReviewView>(`/movies/${movieId}/my-review`, request);
  }

  deleteReview(movieId: number): Observable<void> {
    return this.api.delete<void>(`/movies/${movieId}/my-review`);
  }

  favorites(): Observable<MovieSummary[]> {
    return this.api.get<MovieSummary[]>('/me/favorites');
  }

  addFavorite(movieId: number): Observable<void> {
    return this.api.put<void>(`/me/favorites/${movieId}`, {});
  }

  removeFavorite(movieId: number): Observable<void> {
    return this.api.delete<void>(`/me/favorites/${movieId}`);
  }

  // --- Quản trị ---

  adminReviews(hidden: boolean | null, page = 0, size = 20): Observable<PageResponse<ReviewView>> {
    return this.api.get<PageResponse<ReviewView>>('/admin/reviews', { params: { hidden, page, size } });
  }

  setHidden(id: number, hidden: boolean): Observable<ReviewView> {
    return this.api.put<ReviewView>(`/admin/reviews/${id}/visibility`, { hidden });
  }
}
