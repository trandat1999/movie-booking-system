import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AGE_RATING_COLORS, MovieSummary, MoviePosterComponent } from '@mb/shared';
import { NzTagModule } from 'ng-zorro-antd/tag';

/** Thẻ phim trong lưới danh sách: poster, tên, độ tuổi, thể loại, thời lượng. */
@Component({
  selector: 'app-movie-card',
  imports: [DatePipe, RouterLink, NzTagModule, MoviePosterComponent],
  template: `
    <a class="card" [routerLink]="['/movies', movie().slug]">
      <div class="poster">
        <lib-movie-poster [url]="movie().posterUrl" [title]="movie().title" [width]="200" />
        <nz-tag class="rating" [nzColor]="ratingColors[movie().ageRating]">{{ movie().ageRating }}</nz-tag>
      </div>
      <div class="title">{{ movie().title }}</div>
      <div class="meta">
        {{ genres() }}
      </div>
      <div class="meta">
        {{ movie().durationMin }} phút
        @if (showRelease()) {
          · Khởi chiếu {{ movie().releaseDate | date: 'dd/MM/yyyy' }}
        }
      </div>
    </a>
  `,
  styles: `
    .card {
      display: block;
      color: inherit;
    }

    .poster {
      position: relative;
      overflow: hidden;
      border-radius: 8px;
      aspect-ratio: 2 / 3;
      background: #f0f0f0;

      ::ng-deep img,
      ::ng-deep .placeholder {
        width: 100% !important;
        height: 100% !important;
        font-size: 32px;
        transition: transform 0.2s;
      }
    }

    .card:hover .poster ::ng-deep img,
    .card:hover .poster ::ng-deep .placeholder {
      transform: scale(1.04);
    }

    .rating {
      position: absolute;
      top: 8px;
      left: 8px;
      margin: 0;
    }

    .title {
      margin-top: 8px;
      font-weight: 600;
      line-height: 1.3;
    }

    .meta {
      color: rgba(0, 0, 0, 0.45);
      font-size: 13px;
    }
  `,
})
export class MovieCardComponent {
  readonly movie = input.required<MovieSummary>();
  readonly showRelease = input(false);

  protected readonly ratingColors = AGE_RATING_COLORS;

  protected genres(): string {
    return this.movie()
      .genres.map((g) => g.name)
      .join(', ');
  }
}
