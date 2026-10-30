import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { AGE_RATING_COLORS, MoviePosterComponent, nextDays } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { DayTabsComponent } from '../../../shared/components/day-tabs.component';
import { ShowtimeTimesComponent } from '../../../shared/components/showtime-times.component';
import { groupByMovie } from '../../../shared/showtime-groups';
import { CinemasStore } from '../cinemas.store';

/** Chi tiết rạp và lịch chiếu theo ngày, nhóm theo phim (SCH02, SCH03). */
@Component({
  selector: 'app-cinema-detail-page',
  imports: [
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
  template: `
    @if (store.notFound()) {
      <nz-result nzStatus="404" nzTitle="Không tìm thấy rạp">
        <div nz-result-extra><a nz-button nzType="primary" routerLink="/cinemas">Danh sách rạp</a></div>
      </nz-result>
    }
@if (store.cinema(); as cinema) {
      <section class="header">
        <h1>{{ cinema.name }}</h1>
        <p>{{ cinema.address }}, {{ cinema.city }}</p>
        @if (cinema.phone) {
          <p class="muted">☎ {{ cinema.phone }}</p>
        }
        @if (cinema.description) {
          <p>{{ cinema.description }}</p>
        }
        @for (a of cinema.amenities; track a) {
          <nz-tag>{{ a }}</nz-tag>
        }
      </section>

      <h2>Lịch chiếu</h2>
      <app-day-tabs [days]="days" [selected]="selectedDate()" (selectedChange)="selectedDate.set($event)" />
      @if (store.loadingShowtimes()) {
        <nz-skeleton [nzActive]="true" class="list" />
      } @else {
        <div class="list">
          @for (m of movies(); track m.movieId) {
            <div class="movie">
              <a [routerLink]="['/movies', m.movieSlug]">
                <lib-movie-poster [url]="m.moviePosterUrl" [title]="m.movieTitle" [width]="80" />
              </a>
              <div class="movie-info">
                <a [routerLink]="['/movies', m.movieSlug]" class="title">{{ m.movieTitle }}</a>
                <div class="meta">
                  <nz-tag [nzColor]="ratingColors[m.ageRating]">{{ m.ageRating }}</nz-tag>
                  {{ m.durationMin }} phút
                </div>
                <app-showtime-times [versions]="m.versions" />
              </div>
            </div>
          } @empty {
            <nz-empty nzNotFoundContent="Không có suất chiếu trong ngày này." />
          }
        </div>
      }
    } @else if (!store.notFound()) {
      <nz-skeleton [nzActive]="true" />
    }
  `,
  styles: `
    .header {
      margin-bottom: 32px;

      h1 {
        margin-bottom: 4px;
      }
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .list {
      display: block;
      margin-top: 16px;
    }

    .movie {
      display: flex;
      gap: 16px;
      padding: 16px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .movie-info {
      flex: 1;
      min-width: 0;
    }

    .title {
      font-size: 16px;
      font-weight: 600;
    }

    .meta {
      margin: 4px 0 12px;
      color: rgba(0, 0, 0, 0.45);
    }
  `,
})
export class CinemaDetailPage {
  protected readonly store = inject(CinemasStore);
  private readonly title = inject(Title);

  readonly id = input.required<string>();

  protected readonly ratingColors = AGE_RATING_COLORS;
  protected readonly days = nextDays(7);
  protected readonly selectedDate = signal(this.days[0].date);
  protected readonly movies = computed(() => groupByMovie(this.store.showtimes()));

  constructor() {
    effect(() => this.store.loadCinema(Number(this.id())));
    effect(() => this.store.loadShowtimes(Number(this.id()), this.selectedDate()));
    effect(() => {
      const cinema = this.store.cinema();
      if (cinema) {
        this.title.setTitle(`${cinema.name} — MovieBooking`);
      }
    });
  }
}
