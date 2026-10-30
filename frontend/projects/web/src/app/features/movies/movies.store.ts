import { inject, Injectable, signal } from '@angular/core';
import { CatalogApi, Genre, MovieDetail, MovieSummary, PublicMovieQuery, PublicShowtime } from '@mb/shared';
import { finalize } from 'rxjs';

/** Danh sách phim (MOV02–MOV04, MOV09) và chi tiết phim kèm lịch chiếu (MOV05, MOV06, SCH03). */
@Injectable()
export class MoviesStore {
  private readonly api = inject(CatalogApi);

  // Danh sách
  readonly query = signal<PublicMovieQuery>({ status: 'NOW_SHOWING', page: 0, size: 12 });
  readonly movies = signal<MovieSummary[]>([]);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly genres = signal<Genre[]>([]);
  readonly languages = signal<string[]>([]);

  // Chi tiết
  readonly movie = signal<MovieDetail | null>(null);
  readonly notFound = signal(false);
  readonly showtimes = signal<PublicShowtime[]>([]);
  readonly loadingShowtimes = signal(false);

  loadFilters(): void {
    this.api.listGenres().subscribe((genres) => this.genres.set(genres));
    this.api.listLanguages().subscribe((languages) => this.languages.set(languages));
  }

  search(changes: Partial<PublicMovieQuery>): void {
    this.query.update((q) => ({ ...q, page: 0, ...changes }));
    this.loading.set(true);
    this.api
      .searchMovies(this.query())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => {
        this.movies.set(page.items);
        this.total.set(page.totalElements);
      });
  }

  loadMovie(slug: string): void {
    this.movie.set(null);
    this.notFound.set(false);
    this.api.getMovie(slug).subscribe({
      next: (movie) => this.movie.set(movie),
      error: () => this.notFound.set(true),
    });
  }

  /** @param date yyyy-MM-dd theo giờ Việt Nam */
  loadShowtimes(movieId: number, date: string): void {
    this.loadingShowtimes.set(true);
    this.api
      .searchShowtimes({ movieId, date })
      .pipe(finalize(() => this.loadingShowtimes.set(false)))
      .subscribe((showtimes) => this.showtimes.set(showtimes));
  }
}
