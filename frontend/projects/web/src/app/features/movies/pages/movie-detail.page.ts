import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl, Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import {
  AGE_RATING_COLORS,
  AGE_RATING_LABELS,
  MOVIE_STATUS_LABELS,
  MoviePosterComponent,
  nextDays,
  youtubeEmbedUrl,
} from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { DayTabsComponent } from '../../../shared/components/day-tabs.component';
import { ShowtimeTimesComponent } from '../../../shared/components/showtime-times.component';
import { groupByCinema } from '../../../shared/showtime-groups';
import { MoviesStore } from '../movies.store';

/** Chi tiết phim, trailer và lịch chiếu theo ngày, nhóm theo rạp (MOV05, MOV06, SCH03). */
@Component({
  selector: 'app-movie-detail-page',
  imports: [
    DatePipe,
    RouterLink,
    NzTagModule,
    NzButtonModule,
    NzSkeletonModule,
    NzResultModule,
    NzEmptyModule,
    MoviePosterComponent,
    DayTabsComponent,
    ShowtimeTimesComponent,
  ],
  templateUrl: './movie-detail.page.html',
  styleUrl: './movie-detail.page.less',
})
export class MovieDetailPage {
  protected readonly store = inject(MoviesStore);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly title = inject(Title);

  readonly slug = input.required<string>();

  protected readonly ratingColors = AGE_RATING_COLORS;
  protected readonly ratingLabels = AGE_RATING_LABELS;
  protected readonly statusLabels = MOVIE_STATUS_LABELS;
  protected readonly days = nextDays(7);
  protected readonly selectedDate = signal(this.days[0].date);

  protected readonly cinemas = computed(() => groupByCinema(this.store.showtimes()));

  /** Chỉ nhúng URL YouTube đã kiểm tra bằng youtubeEmbedUrl (chống chèn iframe tùy ý). */
  protected readonly trailer = computed<SafeResourceUrl | null>(() => {
    const embed = youtubeEmbedUrl(this.store.movie()?.trailerUrl);
    return embed ? this.sanitizer.bypassSecurityTrustResourceUrl(embed) : null;
  });

  constructor() {
    effect(() => this.store.loadMovie(this.slug()));
    effect(() => {
      const movie = this.store.movie();
      if (movie) {
        this.title.setTitle(`${movie.title} — MovieBooking`);
        if (movie.status === 'NOW_SHOWING') {
          this.store.loadShowtimes(movie.id, this.selectedDate());
        }
      }
    });
  }

  protected selectDate(date: string): void {
    this.selectedDate.set(date);
  }
}
