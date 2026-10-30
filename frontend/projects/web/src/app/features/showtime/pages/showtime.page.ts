import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  AGE_RATING_COLORS,
  AUDIO_LABELS,
  FORMAT_LABELS,
  GridSeat,
  GridSeatState,
  MoviePosterComponent,
  ROOM_TYPE_LABELS,
  SeatGridComponent,
  SeatStatus,
  VN_OFFSET,
  VndPipe,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { ShowtimeStore } from '../showtime.store';

const STATE_BY_STATUS: Record<SeatStatus, GridSeatState> = {
  AVAILABLE: 'normal',
  HELD: 'held',
  SOLD: 'sold',
  BLOCKED: 'inactive',
};

const MIN_CELL = 18;
const MAX_CELL = 40;

/**
 * Thông tin suất chiếu và sơ đồ ghế theo thời gian hiện tại (SCH05–SCH07, BKG01). Chọn và giữ ghế được bổ sung
 * ở M4.
 */
@Component({
  selector: 'app-showtime-page',
  imports: [
    DatePipe,
    RouterLink,
    VndPipe,
    NzCardModule,
    NzTagModule,
    NzAlertModule,
    NzButtonModule,
    NzResultModule,
    NzSkeletonModule,
    MoviePosterComponent,
    SeatGridComponent,
  ],
  templateUrl: './showtime.page.html',
  styleUrl: './showtime.page.less',
})
export class ShowtimePage {
  protected readonly store = inject(ShowtimeStore);
  private readonly message = inject(NzMessageService);

  readonly id = input.required<string>();

  protected readonly vn = VN_OFFSET;
  protected readonly ratingColors = AGE_RATING_COLORS;
  protected readonly formatLabels = FORMAT_LABELS;
  protected readonly audioLabels = AUDIO_LABELS;
  protected readonly roomTypeLabels = ROOM_TYPE_LABELS;

  /** Ô ghế nhỏ hơn trên màn hình hẹp; người dùng có thể phóng to/thu nhỏ */
  protected readonly cellSize = signal(typeof window !== 'undefined' && window.innerWidth < 576 ? 22 : 30);

  protected readonly colors = computed(() =>
    Object.fromEntries((this.store.seatMap()?.seatTypes ?? []).map((t) => [t.code, t.color])),
  );

  protected readonly gridSeats = computed<GridSeat[]>(() =>
    (this.store.seatMap()?.seats ?? []).map((s) => ({
      gridRow: s.gridRow,
      gridCol: s.gridCol,
      colSpan: s.colSpan,
      seatTypeCode: s.seatTypeCode,
      label: s.rowLabel + s.number,
      state: STATE_BY_STATUS[s.status],
      ref: s,
    })),
  );

  /** Cảnh báo giới hạn độ tuổi theo phân loại phim */
  protected readonly ageWarning = computed(() => {
    const rating = this.store.detail()?.showtime.ageRating;
    switch (rating) {
      case 'T13':
      case 'T16':
      case 'T18':
        return `Phim ${rating}: chỉ dành cho khán giả từ ${rating.slice(1)} tuổi trở lên. Rạp có thể kiểm tra giấy tờ tùy thân.`;
      case 'K':
        return 'Phim K: khán giả dưới 13 tuổi cần xem cùng cha mẹ hoặc người giám hộ.';
      default:
        return null;
    }
  });

  constructor() {
    effect(() => this.store.load(Number(this.id())));
  }

  protected zoom(delta: number): void {
    this.cellSize.update((size) => Math.min(MAX_CELL, Math.max(MIN_CELL, size + delta)));
  }

  protected onSeat(seat: GridSeat): void {
    if (seat.state === 'normal') {
      this.message.info(`Ghế ${seat.label} còn trống — chức năng chọn và giữ ghế sẽ có ở bước đặt vé.`);
    }
  }
}
