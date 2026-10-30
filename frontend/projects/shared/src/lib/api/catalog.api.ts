import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { PageResponse } from '../models/api-response.model';
import { Cinema, Genre, MovieDetail, MovieSummary, ScreenFormat } from '../models/catalog.model';
import { PublicMovieQuery, PublicShowtime, PublicShowtimeDetail, SeatMap } from '../models/public.model';

/** API tra cứu công khai cho web người dùng (không cần đăng nhập). */
@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly api = inject(ApiClient);

  searchMovies(query: PublicMovieQuery): Observable<PageResponse<MovieSummary>> {
    return this.api.get<PageResponse<MovieSummary>>('/movies', {
      params: {
        q: query.q,
        status: query.status,
        genreId: query.genreId,
        ageRating: query.ageRating,
        language: query.language,
        page: query.page,
        size: query.size,
      },
    });
  }

  /** Trang chi tiết tự hiển thị "không tìm thấy" nên không hiện thông báo lỗi chung. */
  getMovie(slug: string): Observable<MovieDetail> {
    return this.api.get<MovieDetail>(`/movies/${encodeURIComponent(slug)}`, { context: skipErrorMessage() });
  }

  listGenres(): Observable<Genre[]> {
    return this.api.get<Genre[]>('/genres');
  }

  listLanguages(): Observable<string[]> {
    return this.api.get<string[]>('/movie-languages');
  }

  listCinemas(): Observable<Cinema[]> {
    return this.api.get<Cinema[]>('/cinemas');
  }

  getCinema(id: number): Observable<Cinema> {
    return this.api.get<Cinema>(`/cinemas/${id}`, { context: skipErrorMessage() });
  }

  /** @param date yyyy-MM-dd theo giờ Việt Nam */
  searchShowtimes(filter: {
    movieId?: number | null;
    cinemaId?: number | null;
    date: string;
    format?: ScreenFormat | null;
  }): Observable<PublicShowtime[]> {
    return this.api.get<PublicShowtime[]>('/showtimes', {
      params: { movieId: filter.movieId, cinemaId: filter.cinemaId, date: filter.date, format: filter.format },
    });
  }

  getShowtime(id: number): Observable<PublicShowtimeDetail> {
    return this.api.get<PublicShowtimeDetail>(`/showtimes/${id}`, { context: skipErrorMessage() });
  }

  getSeatMap(id: number): Observable<SeatMap> {
    return this.api.get<SeatMap>(`/showtimes/${id}/seats`, { context: skipErrorMessage() });
  }
}
