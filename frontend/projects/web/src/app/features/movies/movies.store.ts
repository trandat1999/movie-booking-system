import { inject, Injectable, signal } from '@angular/core';
import {
  AuthStore,
  CatalogApi,
  Genre,
  MovieDetail,
  MovieReviews,
  MovieSummary,
  MyReview,
  PublicMovieQuery,
  PublicShowtime,
  ReviewApi,
  ReviewRequest,
} from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Danh sách phim (MOV02–MOV04, MOV09) và chi tiết phim kèm lịch chiếu (MOV05, MOV06, SCH03). */
@Injectable()
export class MoviesStore {
  private readonly api = inject(CatalogApi);
  private readonly reviewApi = inject(ReviewApi);
  private readonly auth = inject(AuthStore);

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

  // Mở rộng M10: yêu thích, phim tương tự, đánh giá
  readonly favorite = signal(false);
  readonly similar = signal<MovieSummary[]>([]);
  readonly reviews = signal<MovieReviews | null>(null);
  readonly myReview = signal<MyReview | null>(null);
  readonly savingReview = signal(false);

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
      next: (movie) => {
        this.movie.set(movie);
        this.loadExtras(movie);
      },
      error: () => this.notFound.set(true),
    });
  }

  private loadExtras(movie: MovieDetail): void {
    this.similar.set([]);
    this.reviews.set(null);
    this.myReview.set(null);
    this.favorite.set(false);
    this.reviewApi.similarMovies(movie.slug).subscribe((movies) => this.similar.set(movies));
    this.loadReviews(0);
    if (this.auth.user()) {
      this.reviewApi.myReview(movie.id).subscribe((mine) => this.myReview.set(mine));
      this.reviewApi.favorites().subscribe((list) => this.favorite.set(list.some((m) => m.id === movie.id)));
    }
  }

  loadReviews(page: number): void {
    const movie = this.movie();
    if (movie) {
      this.reviewApi.movieReviews(movie.slug, page).subscribe((reviews) => this.reviews.set(reviews));
    }
  }

  toggleFavorite(): Observable<void> {
    const movie = this.movie()!;
    const next = !this.favorite();
    const call = next ? this.reviewApi.addFavorite(movie.id) : this.reviewApi.removeFavorite(movie.id);
    return call.pipe(tap(() => this.favorite.set(next)));
  }

  saveReview(request: ReviewRequest): Observable<unknown> {
    const movie = this.movie()!;
    this.savingReview.set(true);
    return this.reviewApi.saveReview(movie.id, request).pipe(
      tap((review) => {
        this.myReview.update((mine) => ({ eligible: mine?.eligible ?? true, review }));
        this.loadReviews(0);
      }),
      finalize(() => this.savingReview.set(false)),
    );
  }

  deleteReview(): Observable<void> {
    const movie = this.movie()!;
    return this.reviewApi.deleteReview(movie.id).pipe(
      tap(() => {
        this.myReview.update((mine) => (mine ? { ...mine, review: null } : mine));
        this.loadReviews(0);
      }),
    );
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
