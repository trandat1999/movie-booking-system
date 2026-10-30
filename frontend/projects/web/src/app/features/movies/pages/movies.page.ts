import { Component, inject, input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AGE_RATING_LABELS, AgeRating, MovieStatus, toOptions } from '@mb/shared';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzSegmentedModule } from 'ng-zorro-antd/segmented';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';

import { MovieCardComponent } from '../../../shared/components/movie-card.component';
import { MoviesStore } from '../movies.store';

const STATUS_TABS: { label: string; value: MovieStatus }[] = [
  { label: 'Đang chiếu', value: 'NOW_SHOWING' },
  { label: 'Sắp chiếu', value: 'COMING_SOON' },
];

/** Danh sách phim: tab đang chiếu/sắp chiếu, tìm theo tên, lọc thể loại/độ tuổi/ngôn ngữ (MOV02–04, MOV09). */
@Component({
  selector: 'app-movies-page',
  imports: [
    FormsModule,
    NzSegmentedModule,
    NzInputModule,
    NzIconModule,
    NzSelectModule,
    NzPaginationModule,
    NzSkeletonModule,
    MovieCardComponent,
  ],
  templateUrl: './movies.page.html',
  styleUrl: './movies.page.less',
})
export class MoviesPage implements OnInit {
  protected readonly store = inject(MoviesStore);
  private readonly router = inject(Router);

  /** ?status=COMING_SOON (từ trang chủ) */
  readonly status = input<MovieStatus>();

  protected readonly tabs = STATUS_TABS;
  protected readonly ratingOptions = toOptions(AGE_RATING_LABELS);

  protected keyword = '';
  protected genreId: number | null = null;
  protected ageRating: AgeRating | null = null;
  protected language: string | null = null;

  ngOnInit(): void {
    this.store.loadFilters();
    this.store.search({ status: this.status() === 'COMING_SOON' ? 'COMING_SOON' : 'NOW_SHOWING' });
  }

  protected changeTab(status: MovieStatus): void {
    void this.router.navigate([], { queryParams: { status }, replaceUrl: true });
    this.store.search({ status });
  }

  protected applyFilters(): void {
    this.store.search({
      q: this.keyword.trim() || null,
      genreId: this.genreId,
      ageRating: this.ageRating,
      language: this.language,
    });
  }

  protected changePage(page: number): void {
    this.store.search({ page: page - 1 });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
