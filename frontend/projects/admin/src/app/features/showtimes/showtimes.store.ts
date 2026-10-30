import { inject, Injectable, signal } from '@angular/core';
import {
  AdminCatalogApi,
  AdminCinemaApi,
  AdminShowtimeApi,
  Cinema,
  MovieSummary,
  PriceQuote,
  Room,
  ScreenFormat,
  Showtime,
  ShowtimeQuery,
  ShowtimeRequest,
  ShowtimeUpdateRequest,
} from '@mb/shared';
import { finalize, map, Observable, of, tap } from 'rxjs';

import { todayInVietnam } from './vn-time';

/** Trạng thái trang lịch chiếu (SCHADM). */
@Injectable()
export class ShowtimesStore {
  private readonly api = inject(AdminShowtimeApi);
  private readonly cinemaApi = inject(AdminCinemaApi);
  private readonly catalogApi = inject(AdminCatalogApi);

  readonly query = signal<ShowtimeQuery>({
    cinemaId: null,
    roomId: null,
    movieId: null,
    date: todayInVietnam(),
    status: null,
    page: 0,
    size: 50,
  });
  readonly showtimes = signal<Showtime[]>([]);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly saving = signal(false);

  readonly cinemas = signal<Cinema[]>([]);
  /** Phim chưa ngừng chiếu (để lên lịch) */
  readonly movies = signal<MovieSummary[]>([]);
  private readonly roomsCache = new Map<number, Room[]>();

  load(): void {
    this.loading.set(true);
    this.api
      .search(this.query())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => {
        this.showtimes.set(page.items);
        this.total.set(page.totalElements);
      });
  }

  search(filters: Partial<ShowtimeQuery>): void {
    this.query.update((q) => ({ ...q, ...filters, page: 0 }));
    this.load();
  }

  changePage(pageIndex: number): void {
    this.query.update((q) => ({ ...q, page: pageIndex - 1 }));
    this.load();
  }

  loadOptions(): void {
    this.cinemaApi.listCinemas().subscribe((cinemas) => this.cinemas.set(cinemas));
    this.catalogApi
      .searchMovies({ page: 0, size: 100 })
      .subscribe((page) => this.movies.set(page.items.filter((m) => m.status !== 'ENDED')));
  }

  rooms(cinemaId: number): Observable<Room[]> {
    const cached = this.roomsCache.get(cinemaId);
    if (cached) {
      return of(cached);
    }
    return this.cinemaApi.listRooms(cinemaId).pipe(
      map((rooms) => rooms.filter((r) => r.seatCount > 0)),
      tap((rooms) => this.roomsCache.set(cinemaId, rooms)),
    );
  }

  quote(roomId: number, movieId: number | null, format: ScreenFormat, startAt: string): Observable<PriceQuote> {
    return this.api.quote(roomId, movieId, format, startAt);
  }

  save(id: number | null, request: ShowtimeRequest): Observable<Showtime> {
    this.saving.set(true);
    const update: ShowtimeUpdateRequest = {
      startAt: request.startAt,
      format: request.format,
      audioType: request.audioType,
      prices: request.prices,
    };
    const call = id === null ? this.api.create(request) : this.api.update(id, update);
    return call.pipe(
      tap(() => this.load()),
      finalize(() => this.saving.set(false)),
    );
  }

  cancel(id: number): Observable<Showtime> {
    return this.api.cancel(id).pipe(tap(() => this.load()));
  }
}
