import { inject, Injectable, signal } from '@angular/core';
import { AdminReportApi, CatalogApi, Cinema, GroupStats, MovieSummary, OverviewStats, StatsFilter } from '@mb/shared';
import { finalize, forkJoin } from 'rxjs';

/** Dashboard (ADM01–ADM03): tổng quan bán vé, thống kê nhóm, xuất CSV. */
@Injectable()
export class DashboardStore {
  private readonly api = inject(AdminReportApi);
  private readonly catalogApi = inject(CatalogApi);

  readonly overview = signal<OverviewStats | null>(null);
  readonly groups = signal<GroupStats | null>(null);
  readonly cinemas = signal<Cinema[]>([]);
  readonly movies = signal<MovieSummary[]>([]);
  readonly loading = signal(false);
  readonly exporting = signal(false);

  loadOptions(): void {
    forkJoin({
      cinemas: this.catalogApi.listCinemas(),
      now: this.catalogApi.searchMovies({ status: 'NOW_SHOWING', page: 0, size: 100 }),
      ended: this.catalogApi.searchMovies({ status: 'ENDED', page: 0, size: 100 }),
    }).subscribe(({ cinemas, now, ended }) => {
      this.cinemas.set(cinemas);
      this.movies.set([...now.items, ...ended.items]);
    });
  }

  load(filter: StatsFilter): void {
    this.loading.set(true);
    forkJoin({ overview: this.api.overview(filter), groups: this.api.groupStats(filter) })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe(({ overview, groups }) => {
        this.overview.set(overview);
        this.groups.set(groups);
      });
  }

  /** Tải CSV về máy (Excel mở được, UTF-8 có BOM). */
  exportCsv(filter: StatsFilter): void {
    this.exporting.set(true);
    this.api
      .exportOrdersCsv(filter)
      .pipe(finalize(() => this.exporting.set(false)))
      .subscribe((blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `don-hang_${filter.from ?? ''}_${filter.to ?? ''}.csv`;
        a.click();
        URL.revokeObjectURL(url);
      });
  }
}
