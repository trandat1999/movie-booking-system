import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { PageResponse } from '../models/api-response.model';
import { Genre, MovieDetail, MovieQuery, MovieRequest, MovieSummary, Person } from '../models/catalog.model';

/** Quản trị phim, thể loại, đạo diễn/diễn viên. */
@Injectable({ providedIn: 'root' })
export class AdminCatalogApi {
  private readonly api = inject(ApiClient);

  listGenres(): Observable<Genre[]> {
    return this.api.get<Genre[]>('/admin/genres');
  }

  createGenre(name: string): Observable<Genre> {
    return this.api.post<Genre>('/admin/genres', { name });
  }

  updateGenre(id: number, name: string): Observable<Genre> {
    return this.api.put<Genre>(`/admin/genres/${id}`, { name });
  }

  deleteGenre(id: number): Observable<void> {
    return this.api.delete<void>(`/admin/genres/${id}`);
  }

  searchPeople(q: string | null, page = 0, size = 20): Observable<PageResponse<Person>> {
    return this.api.get<PageResponse<Person>>('/admin/people', { params: { q, page, size } });
  }

  createPerson(fullName: string, avatarUrl: string | null): Observable<Person> {
    return this.api.post<Person>('/admin/people', { fullName, avatarUrl });
  }

  updatePerson(id: number, fullName: string, avatarUrl: string | null): Observable<Person> {
    return this.api.put<Person>(`/admin/people/${id}`, { fullName, avatarUrl });
  }

  deletePerson(id: number): Observable<void> {
    return this.api.delete<void>(`/admin/people/${id}`);
  }

  searchMovies(query: MovieQuery): Observable<PageResponse<MovieSummary>> {
    return this.api.get<PageResponse<MovieSummary>>('/admin/movies', {
      params: { q: query.q, status: query.status, genreId: query.genreId, page: query.page, size: query.size },
    });
  }

  getMovie(id: number): Observable<MovieDetail> {
    return this.api.get<MovieDetail>(`/admin/movies/${id}`);
  }

  createMovie(request: MovieRequest): Observable<MovieDetail> {
    return this.api.post<MovieDetail>('/admin/movies', request);
  }

  updateMovie(id: number, request: MovieRequest): Observable<MovieDetail> {
    return this.api.put<MovieDetail>(`/admin/movies/${id}`, request);
  }

  deleteMovie(id: number): Observable<void> {
    return this.api.delete<void>(`/admin/movies/${id}`);
  }
}
