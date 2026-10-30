import { computed, inject, Injectable, signal } from '@angular/core';
import { CatalogApi, Cinema, PublicShowtime } from '@mb/shared';
import { finalize } from 'rxjs';

/** Danh sách rạp, chi tiết rạp và lịch chiếu tại rạp (SCH01–SCH03). */
@Injectable()
export class CinemasStore {
  private readonly api = inject(CatalogApi);

  readonly cinemas = signal<Cinema[]>([]);
  readonly loading = signal(false);
  readonly cinema = signal<Cinema | null>(null);
  readonly notFound = signal(false);
  readonly showtimes = signal<PublicShowtime[]>([]);
  readonly loadingShowtimes = signal(false);

  /** Rạp nhóm theo thành phố */
  readonly byCity = computed(() => {
    const groups = new Map<string, Cinema[]>();
    this.cinemas().forEach((c) => groups.set(c.city, [...(groups.get(c.city) ?? []), c]));
    return [...groups.entries()].map(([city, cinemas]) => ({ city, cinemas }));
  });

  loadCinemas(): void {
    this.loading.set(true);
    this.api
      .listCinemas()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((cinemas) => this.cinemas.set(cinemas));
  }

  loadCinema(id: number): void {
    this.cinema.set(null);
    this.notFound.set(false);
    this.api.getCinema(id).subscribe({
      next: (cinema) => this.cinema.set(cinema),
      error: () => this.notFound.set(true),
    });
  }

  /** @param date yyyy-MM-dd theo giờ Việt Nam */
  loadShowtimes(cinemaId: number, date: string): void {
    this.loadingShowtimes.set(true);
    this.api
      .searchShowtimes({ cinemaId, date })
      .pipe(finalize(() => this.loadingShowtimes.set(false)))
      .subscribe((showtimes) => this.showtimes.set(showtimes));
  }
}
