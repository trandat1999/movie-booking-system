import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ApiResponse,
  GridCell,
  GridSeat,
  resolveErrorMessage,
  ROOM_TYPE_LABELS,
  Seat,
  SeatCell,
  SeatGridComponent,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CinemasStore } from '../cinemas.store';
import { eraseSeat, fitToGrid, placeSeat, quickLayout, SpanLookup } from '../layout-editor';

const ERASE = 'ERASE';

/**
 * Trình thiết kế sơ đồ ghế dạng lưới (CIN04, CIN07, CIN08). Phòng đã có suất chiếu thì sơ đồ bị khóa, chỉ bật/tắt
 * bán từng ghế (CIN06).
 */
@Component({
  selector: 'adm-room-layout-page',
  imports: [
    FormsModule,
    RouterLink,
    NzCardModule,
    NzButtonModule,
    NzRadioModule,
    NzInputModule,
    NzInputNumberModule,
    NzCheckboxModule,
    NzFormModule,
    NzAlertModule,
    NzTagModule,
    NzSkeletonModule,
    NzPopconfirmModule,
    SeatGridComponent,
  ],
  templateUrl: './room-layout.page.html',
  styleUrl: './room-layout.page.less',
})
export class RoomLayoutPage implements OnInit {
  protected readonly store = inject(CinemasStore);
  private readonly message = inject(NzMessageService);

  readonly cinemaId = input.required<string>();
  readonly roomId = input.required<string>();

  protected readonly erase = ERASE;
  protected readonly roomTypeLabels = ROOM_TYPE_LABELS;

  // Bản nháp đang vẽ
  protected readonly gridRows = signal(8);
  protected readonly gridCols = signal(12);
  protected readonly draft = signal<SeatCell[]>([]);
  protected readonly dirty = signal(false);
  protected brush = 'STANDARD';
  protected readonly errors = signal<string[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  // Tạo nhanh
  protected quick = { rows: 8, seatsPerRow: 12, aisles: '3, 9', vipFrom: 3, vipTo: 5, coupleLastRow: true };

  protected readonly layout = this.store.layout;
  protected readonly locked = computed(() => this.layout()?.room.layoutLocked ?? false);
  protected readonly seatTypes = computed(() => this.layout()?.seatTypes ?? []);
  protected readonly spans = computed<SpanLookup>(() =>
    Object.fromEntries(this.seatTypes().map((t) => [t.code, t.colSpan])),
  );
  protected readonly colors = computed(() => Object.fromEntries(this.seatTypes().map((t) => [t.code, t.color])));

  /** Ghế hiển thị: sơ đồ đã lưu (khi khóa hoặc chưa sửa) hoặc bản nháp. */
  protected readonly gridSeats = computed<GridSeat[]>(() => {
    const layout = this.layout();
    if (layout && !this.dirty()) {
      return layout.seats.map((s) => ({
        gridRow: s.gridRow,
        gridCol: s.gridCol,
        colSpan: s.colSpan,
        seatTypeCode: s.seatTypeCode,
        label: s.rowLabel + s.number,
        state: s.active ? 'normal' : 'inactive',
        ref: s,
      }));
    }
    return this.draft().map((s) => ({ ...s, colSpan: this.spans()[s.seatTypeCode] ?? 1 }));
  });

  protected readonly summary = computed(() => {
    const counts = new Map<string, number>();
    let capacity = 0;
    for (const seat of this.gridSeats()) {
      counts.set(seat.seatTypeCode, (counts.get(seat.seatTypeCode) ?? 0) + 1);
      capacity += this.seatTypes().find((t) => t.code === seat.seatTypeCode)?.capacity ?? 1;
    }
    return { counts, total: this.gridSeats().length, capacity };
  });

  constructor() {
    // Nạp sơ đồ đã lưu vào bản nháp mỗi khi tải lại
    effect(() => {
      const layout = this.layout();
      if (layout && String(layout.room.id) === this.roomId()) {
        this.gridRows.set(layout.room.gridRows || 8);
        this.gridCols.set(layout.room.gridCols || 12);
        this.draft.set(layout.seats.map((s) => ({ gridRow: s.gridRow, gridCol: s.gridCol, seatTypeCode: s.seatTypeCode })));
        this.dirty.set(false);
      }
    });
  }

  ngOnInit(): void {
    this.store.layout.set(null);
    this.store.loadLayout(Number(this.roomId()));
  }

  protected onCell(cell: GridCell): void {
    if (this.locked() || this.brush === ERASE) {
      return;
    }
    this.apply(placeSeat(this.draft(), cell.gridRow, cell.gridCol, this.brush, this.spans(), this.gridCols()));
  }

  protected onSeat(seat: GridSeat): void {
    if (this.locked()) {
      this.toggleActive(seat.ref as Seat);
      return;
    }
    const next =
      this.brush === ERASE
        ? eraseSeat(this.draft(), seat.gridRow, seat.gridCol, this.spans())
        : placeSeat(this.draft(), seat.gridRow, seat.gridCol, this.brush, this.spans(), this.gridCols());
    this.apply(next);
  }

  protected resize(rows: number, cols: number): void {
    this.gridRows.set(rows);
    this.gridCols.set(cols);
    this.draft.set(fitToGrid(this.draft(), rows, cols, this.spans()));
    this.dirty.set(true);
  }

  protected generate(): void {
    const layout = quickLayout({
      rows: this.quick.rows,
      seatsPerRow: this.quick.seatsPerRow,
      aisleAfter: this.quick.aisles
        .split(',')
        .map((s) => Number(s.trim()))
        .filter((n) => Number.isInteger(n)),
      vipFrom: this.quick.vipFrom,
      vipTo: this.quick.vipTo,
      coupleLastRow: this.quick.coupleLastRow,
    });
    if (layout.gridCols > 40 || layout.gridRows > 30) {
      this.message.warning('Lưới tối đa 30 hàng × 40 cột');
      return;
    }
    this.gridRows.set(layout.gridRows);
    this.gridCols.set(layout.gridCols);
    this.draft.set(layout.seats);
    this.dirty.set(true);
  }

  protected clear(): void {
    this.draft.set([]);
    this.dirty.set(true);
  }

  protected reset(): void {
    this.store.loadLayout(Number(this.roomId()));
    this.errors.set([]);
    this.errorMessage.set(null);
  }

  protected save(): void {
    this.errors.set([]);
    this.errorMessage.set(null);
    this.store
      .saveLayout(Number(this.roomId()), { gridRows: this.gridRows(), gridCols: this.gridCols(), seats: this.draft() })
      .subscribe({
        next: (layout) => this.message.success(`Đã lưu sơ đồ: ${layout.room.seatCount} ghế, ${layout.room.capacity} chỗ`),
        error: (err: HttpErrorResponse) => {
          this.errorMessage.set(resolveErrorMessage(err));
          const details = (err.error as ApiResponse<unknown> | null)?.error?.details;
          this.errors.set(Array.isArray(details) ? (details as string[]) : []);
        },
      });
  }

  private apply(next: SeatCell[] | null): void {
    if (next === null) {
      this.message.warning('Không đủ chỗ trống để đặt ghế này');
      return;
    }
    this.draft.set(next);
    this.dirty.set(true);
  }

  private toggleActive(seat: Seat): void {
    this.store.setSeatActive(Number(this.roomId()), seat, !seat.active).subscribe((updated) =>
      this.message.success(
        `Ghế ${updated.rowLabel}${updated.number}: ${updated.active ? 'mở bán lại' : 'ngừng bán'}`,
      ),
    );
  }
}
