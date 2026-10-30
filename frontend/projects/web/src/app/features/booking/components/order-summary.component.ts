import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  AGE_RATING_COLORS,
  AUDIO_LABELS,
  FORMAT_LABELS,
  MoviePosterComponent,
  Order,
  VN_OFFSET,
  VndPipe,
} from '@mb/shared';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

/** Thông tin suất chiếu + danh sách ghế và giá của một đơn. */
@Component({
  selector: 'app-order-summary',
  imports: [DatePipe, RouterLink, VndPipe, NzTableModule, NzTagModule, MoviePosterComponent],
  template: `
    @let s = order().showtime;
    <section class="showtime">
      <lib-movie-poster [url]="s.posterUrl" [title]="s.movieTitle" [width]="80" />
      <div class="info">
        <a [routerLink]="['/showtimes', s.id]" class="title">{{ s.movieTitle }}</a>
        <div>
          <nz-tag [nzColor]="ratingColors[s.ageRating]">{{ s.ageRating }}</nz-tag>
          <nz-tag>{{ formatLabels[s.format] }} · {{ audioLabels[s.audioType] }}</nz-tag>
        </div>
        <div class="when">{{ s.startAt | date: 'HH:mm, EEEE dd/MM/yyyy' : vn }}</div>
        <div class="muted">{{ s.cinemaName }} · {{ s.roomName }} — {{ s.cinemaAddress }}</div>
      </div>
    </section>
    <nz-table [nzData]="order().items" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
      <thead>
        <tr>
          <th>Ghế</th>
          <th>Loại ghế</th>
          <th nzAlign="right">Giá</th>
        </tr>
      </thead>
      <tbody>
        @for (item of order().items; track item.seatId) {
          <tr>
            <td>{{ item.label }}</td>
            <td>{{ item.seatTypeCode }}</td>
            <td nzAlign="right">{{ item.price | vnd }}</td>
          </tr>
        }
        <tr class="total-row">
          <td colspan="2">Tổng cộng ({{ order().items.length }} ghế)</td>
          <td nzAlign="right">{{ order().total | vnd }}</td>
        </tr>
      </tbody>
    </nz-table>
  `,
  styles: `
    .showtime {
      display: flex;
      gap: 16px;
      margin-bottom: 16px;
    }

    .info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .title {
      font-size: 18px;
      font-weight: 600;
    }

    .when {
      font-weight: 500;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .total-row td {
      font-weight: 600;
      font-size: 16px;
    }
  `,
})
export class OrderSummaryComponent {
  readonly order = input.required<Order>();

  protected readonly vn = VN_OFFSET;
  protected readonly ratingColors = AGE_RATING_COLORS;
  protected readonly formatLabels = FORMAT_LABELS;
  protected readonly audioLabels = AUDIO_LABELS;
}
