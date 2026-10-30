import { inject, Injectable, signal } from '@angular/core';
import { CatalogApi, MovieSummary } from '@mb/shared';
import { finalize, forkJoin } from 'rxjs';

/** Trang chủ (MOV01): phim nổi bật, đang chiếu, sắp chiếu. */
@Injectable()
export class HomeStore {
  private readonly api = inject(CatalogApi);

  readonly nowShowing = signal<MovieSummary[]>([]);
  readonly comingSoon = signal<MovieSummary[]>([]);
  readonly loading = signal(false);

  load(): void {
    this.loading.set(true);
    forkJoin({
      now: this.api.searchMovies({ status: 'NOW_SHOWING', page: 0, size: 10 }),
      soon: this.api.searchMovies({ status: 'COMING_SOON', page: 0, size: 5 }),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ now, soon }) => {
        this.nowShowing.set(now.items);
        this.comingSoon.set(soon.items);
      });
  }
}
