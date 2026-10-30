import { inject, Injectable, signal } from '@angular/core';
import { CatalogApi, PublicShowtimeDetail, SeatMap } from '@mb/shared';
import { forkJoin } from 'rxjs';

/** Thông tin suất chiếu và sơ đồ ghế (SCH05–SCH07, BKG01). */
@Injectable()
export class ShowtimeStore {
  private readonly api = inject(CatalogApi);

  readonly detail = signal<PublicShowtimeDetail | null>(null);
  readonly seatMap = signal<SeatMap | null>(null);
  readonly notFound = signal(false);

  load(showtimeId: number): void {
    this.detail.set(null);
    this.seatMap.set(null);
    this.notFound.set(false);
    forkJoin({ detail: this.api.getShowtime(showtimeId), seatMap: this.api.getSeatMap(showtimeId) }).subscribe({
      next: ({ detail, seatMap }) => {
        this.detail.set(detail);
        this.seatMap.set(seatMap);
      },
      error: () => this.notFound.set(true),
    });
  }
}
