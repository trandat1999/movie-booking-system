import { inject, Injectable, signal } from '@angular/core';
import { MovieSummary, ReviewApi } from '@mb/shared';
import { finalize } from 'rxjs';

/** Danh sách phim yêu thích (MOV04). */
@Injectable()
export class FavoritesStore {
  private readonly api = inject(ReviewApi);

  readonly movies = signal<MovieSummary[]>([]);
  readonly loading = signal(false);

  load(): void {
    this.loading.set(true);
    this.api
      .favorites()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((movies) => this.movies.set(movies));
  }
}
