import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ApiResponse,
  AUDIO_LABELS,
  AudioType,
  ConflictingShowtime,
  DAY_TYPE_LABELS,
  FORMAT_LABELS,
  fromIsoDate,
  isoToVnParts,
  PHASE_COLORS,
  PHASE_LABELS,
  PriceQuote,
  resolveErrorMessage,
  Room,
  ScreenFormat,
  Showtime,
  toIsoDate,
  toOptions,
  VN_OFFSET,
  vnDateTimeToIso,
  VndPipe,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTimePickerModule } from 'ng-zorro-antd/time-picker';

import { ShowtimesStore } from '../showtimes.store';

interface ShowtimeForm {
  movieId: number | null;
  cinemaId: number | null;
  roomId: number | null;
  date: Date | null;
  time: Date | null;
  format: ScreenFormat;
  audioType: AudioType;
}

/** Lịch chiếu: lọc theo ngày/rạp/phòng/phim, tạo – sửa – hủy suất (SCHADM01–09). */
@Component({
  selector: 'adm-showtimes-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    VndPipe,
    NzCardModule,
    NzTableModule,
    NzSelectModule,
    NzDatePickerModule,
    NzTimePickerModule,
    NzButtonModule,
    NzTagModule,
    NzProgressModule,
    NzPopconfirmModule,
    NzModalModule,
    NzFormModule,
    NzInputNumberModule,
    NzAlertModule,
  ],
  templateUrl: './showtimes.page.html',
  styleUrl: './showtimes.page.less',
})
export class ShowtimesPage implements OnInit {
  protected readonly store = inject(ShowtimesStore);
  private readonly message = inject(NzMessageService);

  protected readonly vn = VN_OFFSET;
  protected readonly formatLabels = FORMAT_LABELS;
  protected readonly audioLabels = AUDIO_LABELS;
  protected readonly dayTypeLabels = DAY_TYPE_LABELS;
  protected readonly phaseLabels = PHASE_LABELS;
  protected readonly phaseColors = PHASE_COLORS;
  protected readonly formatOptions = toOptions(FORMAT_LABELS);
  protected readonly audioOptions = toOptions(AUDIO_LABELS);

  // Bộ lọc
  protected filterDate: Date | null = fromIsoDate(this.store.query().date);
  protected filterCinemaId: number | null = null;
  protected filterRoomId: number | null = null;
  protected filterMovieId: number | null = null;
  protected readonly filterRooms = signal<Room[]>([]);

  // Form tạo/sửa: undefined = đóng, null = tạo mới
  protected readonly editing = signal<Showtime | null | undefined>(undefined);
  protected form: ShowtimeForm = this.emptyForm();
  protected readonly formRooms = signal<Room[]>([]);
  protected readonly quote = signal<PriceQuote | null>(null);
  /** Giá đang nhập theo mã loại ghế (mặc định = giá tính từ bảng giá) */
  protected prices: Record<string, number> = {};
  protected readonly formError = signal<string | null>(null);
  protected readonly conflicts = signal<ConflictingShowtime[]>([]);

  protected readonly canSubmit = computed(() => this.quote() !== null);

  protected readonly vndFormat = (value: number): string =>
    value === null || value === undefined ? '' : `${new Intl.NumberFormat('vi-VN').format(value)} ₫`;
  protected readonly vndParse = (value: string): number => Number(value.replace(/\D/g, ''));

  ngOnInit(): void {
    this.store.loadOptions();
    this.store.load();
  }

  // ---------- Bộ lọc ----------
  protected onFilterCinema(cinemaId: number | null): void {
    this.filterRoomId = null;
    this.filterRooms.set([]);
    if (cinemaId) {
      this.store.rooms(cinemaId).subscribe((rooms) => this.filterRooms.set(rooms));
    }
    this.applyFilters();
  }

  protected applyFilters(): void {
    this.store.search({
      date: toIsoDate(this.filterDate),
      cinemaId: this.filterCinemaId,
      roomId: this.filterRoomId,
      movieId: this.filterMovieId,
    });
  }

  protected shiftDay(days: number): void {
    const base = this.filterDate ?? new Date();
    this.filterDate = new Date(base.getFullYear(), base.getMonth(), base.getDate() + days);
    this.applyFilters();
  }

  protected occupancy(s: Showtime): number {
    const sellable = s.seats.total - s.seats.blocked;
    return sellable ? Math.round(((s.seats.sold + s.seats.held) / sellable) * 100) : 0;
  }

  protected editable(s: Showtime): boolean {
    return s.phase === 'UPCOMING' && s.seats.sold === 0 && s.seats.held === 0;
  }

  protected cancel(s: Showtime): void {
    this.store.cancel(s.id).subscribe(() => this.message.success('Đã hủy suất chiếu'));
  }

  // ---------- Form ----------
  protected open(s: Showtime | null): void {
    this.formError.set(null);
    this.conflicts.set([]);
    this.quote.set(null);
    this.prices = {};
    if (s) {
      const parts = isoToVnParts(s.startAt);
      const [h, m] = parts.time.split(':').map(Number);
      this.form = {
        movieId: s.movieId,
        cinemaId: s.cinemaId,
        roomId: s.roomId,
        date: fromIsoDate(parts.date),
        time: new Date(2000, 0, 1, h, m),
        format: s.format,
        audioType: s.audioType,
      };
      this.store.rooms(s.cinemaId).subscribe((rooms) => this.formRooms.set(rooms));
    } else {
      this.form = this.emptyForm();
      this.formRooms.set([]);
    }
    this.editing.set(s);
    this.refreshQuote(s ? Object.fromEntries(s.prices.map((p) => [p.seatTypeCode, p.price])) : undefined);
  }

  protected onFormCinema(cinemaId: number | null): void {
    this.form.roomId = null;
    this.formRooms.set([]);
    if (cinemaId) {
      this.store.rooms(cinemaId).subscribe((rooms) => this.formRooms.set(rooms));
    }
    this.refreshQuote();
  }

  /** Tính lại giờ kết thúc và giá khi đổi phim/phòng/định dạng/giờ chiếu. */
  protected refreshQuote(keepPrices?: Record<string, number>): void {
    const startAt = this.startAtIso();
    if (!this.form.roomId || !startAt) {
      this.quote.set(null);
      return;
    }
    this.store.quote(this.form.roomId, this.form.movieId, this.form.format, startAt).subscribe((quote) => {
      this.quote.set(quote);
      this.prices = Object.fromEntries(quote.prices.map((p) => [p.seatTypeCode, keepPrices?.[p.seatTypeCode] ?? p.price]));
    });
  }

  protected submit(): void {
    const startAt = this.startAtIso();
    const quote = this.quote();
    if (!this.form.movieId || !this.form.roomId || !startAt || !quote) {
      this.formError.set('Vui lòng chọn đủ phim, phòng, ngày và giờ chiếu');
      return;
    }
    // Chỉ gửi giá khác với bảng giá
    const overrides = Object.fromEntries(
      quote.prices.filter((p) => this.prices[p.seatTypeCode] !== p.price).map((p) => [p.seatTypeCode, this.prices[p.seatTypeCode]]),
    );
    this.formError.set(null);
    this.conflicts.set([]);
    this.store
      .save(this.editing()?.id ?? null, {
        movieId: this.form.movieId,
        roomId: this.form.roomId,
        startAt,
        format: this.form.format,
        audioType: this.form.audioType,
        prices: Object.keys(overrides).length ? overrides : null,
      })
      .subscribe({
        next: () => {
          this.message.success(this.editing() ? 'Đã cập nhật suất chiếu' : 'Đã tạo suất chiếu');
          this.editing.set(undefined);
        },
        error: (err: HttpErrorResponse) => {
          this.formError.set(resolveErrorMessage(err));
          const body = err.error as ApiResponse<unknown> | null;
          if (body?.error?.code === 'SHOWTIME_OVERLAP' && Array.isArray(body.error.details)) {
            this.conflicts.set(body.error.details as ConflictingShowtime[]);
          }
        },
      });
  }

  private startAtIso(): string | null {
    const date = toIsoDate(this.form.date);
    if (!date || !this.form.time) {
      return null;
    }
    const time = `${String(this.form.time.getHours()).padStart(2, '0')}:${String(this.form.time.getMinutes()).padStart(2, '0')}`;
    return vnDateTimeToIso(date, time);
  }

  private emptyForm(): ShowtimeForm {
    return {
      movieId: null,
      cinemaId: this.filterCinemaId ?? null,
      roomId: null,
      date: this.filterDate ?? new Date(),
      time: new Date(2000, 0, 1, 19, 0),
      format: 'TWO_D',
      audioType: 'SUBTITLED',
    };
  }
}
