import { inject, Injectable, signal } from '@angular/core';
import { AdminCatalogApi, Genre, MovieDetail, MovieQuery, MovieRequest, MovieSummary, Person } from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Trạng thái các trang quản lý phim, thể loại, đạo diễn/diễn viên (MOVADM). */
@Injectable()
export class CatalogStore {
  private readonly api = inject(AdminCatalogApi);

  // Phim
  readonly movieQuery = signal<MovieQuery>({ q: null, status: null, genreId: null, page: 0, size: 20 });
  readonly movies = signal<MovieSummary[]>([]);
  readonly movieTotal = signal(0);
  readonly loadingMovies = signal(false);

  // Thể loại
  readonly genres = signal<Genre[]>([]);

  // Người
  readonly people = signal<Person[]>([]);
  readonly peopleTotal = signal(0);
  readonly peopleQuery = signal({ q: '', page: 0, size: 20 });
  readonly loadingPeople = signal(false);

  readonly saving = signal(false);

  loadMovies(): void {
    this.loadingMovies.set(true);
    this.api
      .searchMovies(this.movieQuery())
      .pipe(finalize(() => this.loadingMovies.set(false)))
      .subscribe((page) => {
        this.movies.set(page.items);
        this.movieTotal.set(page.totalElements);
      });
  }

  searchMovies(filters: Pick<MovieQuery, 'q' | 'status' | 'genreId'>): void {
    this.movieQuery.update((q) => ({ ...q, ...filters, page: 0 }));
    this.loadMovies();
  }

  changeMoviePage(pageIndex: number, size: number): void {
    this.movieQuery.update((q) => ({ ...q, page: size === q.size ? pageIndex - 1 : 0, size }));
    this.loadMovies();
  }

  getMovie(id: number): Observable<MovieDetail> {
    return this.api.getMovie(id);
  }

  saveMovie(id: number | null, request: MovieRequest): Observable<MovieDetail> {
    this.saving.set(true);
    const call = id === null ? this.api.createMovie(request) : this.api.updateMovie(id, request);
    return call.pipe(finalize(() => this.saving.set(false)));
  }

  deleteMovie(id: number): Observable<void> {
    return this.api.deleteMovie(id).pipe(tap(() => this.loadMovies()));
  }

  loadGenres(): void {
    this.api.listGenres().subscribe((genres) => this.genres.set(genres));
  }

  saveGenre(id: number | null, name: string): Observable<Genre> {
    this.saving.set(true);
    const call = id === null ? this.api.createGenre(name) : this.api.updateGenre(id, name);
    return call.pipe(
      tap(() => this.loadGenres()),
      finalize(() => this.saving.set(false)),
    );
  }

  deleteGenre(id: number): Observable<void> {
    return this.api.deleteGenre(id).pipe(tap(() => this.loadGenres()));
  }

  loadPeople(): void {
    const { q, page, size } = this.peopleQuery();
    this.loadingPeople.set(true);
    this.api
      .searchPeople(q || null, page, size)
      .pipe(finalize(() => this.loadingPeople.set(false)))
      .subscribe((result) => {
        this.people.set(result.items);
        this.peopleTotal.set(result.totalElements);
      });
  }

  searchPeople(q: string): void {
    this.peopleQuery.update((query) => ({ ...query, q, page: 0 }));
    this.loadPeople();
  }

  changePeoplePage(pageIndex: number): void {
    this.peopleQuery.update((query) => ({ ...query, page: pageIndex - 1 }));
    this.loadPeople();
  }

  /** Tìm nhanh cho ô chọn đạo diễn/diễn viên. */
  lookupPeople(q: string): Observable<{ items: Person[] }> {
    return this.api.searchPeople(q || null, 0, 20);
  }

  savePerson(id: number | null, fullName: string, avatarUrl: string | null): Observable<Person> {
    this.saving.set(true);
    const call =
      id === null ? this.api.createPerson(fullName, avatarUrl) : this.api.updatePerson(id, fullName, avatarUrl);
    return call.pipe(
      tap(() => this.loadPeople()),
      finalize(() => this.saving.set(false)),
    );
  }

  deletePerson(id: number): Observable<void> {
    return this.api.deletePerson(id).pipe(tap(() => this.loadPeople()));
  }
}
