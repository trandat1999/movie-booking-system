import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  GridSeat,
  GridSeatState,
  SeatGridComponent,
  SeatMapSeat,
  SeatStatus,
  VN_OFFSET,
  VndPipe,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { GroupSeatsStore } from '../group-seats.store';

const SEAT_STATE: Record<SeatStatus, GridSeatState> = {
  AVAILABLE: 'normal',
  HELD: 'held',
  SOLD: 'sold',
  BLOCKED: 'inactive',
};

const REASONS: Record<string, string> = {
  NOT_ENOUGH_SEATS: 'Suất chiếu không còn đủ ghế trống cho cả nhóm.',
  NO_CLUSTER: 'Không còn cụm ghế gần nhau đủ cho cả nhóm. Hãy cho phép chia hàng, đổi loại ghế hoặc mở chốt để chọn suất khác.',
};

/** Chọn ghế cho nhóm (GRP10–GRP14) — chỉ trưởng nhóm. */
@Component({
  selector: 'app-group-seats-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    VndPipe,
    NzAlertModule,
    NzButtonModule,
    NzCardModule,
    NzCheckboxModule,
    NzEmptyModule,
    NzInputNumberModule,
    NzResultModule,
    NzSelectModule,
    NzSkeletonModule,
    NzSpinModule,
    NzTagModule,
    SeatGridComponent,
  ],
  template: `
    @if (store.notFound()) {
      <nz-result nzStatus="404" nzTitle="Không tìm thấy nhóm">
        <div nz-result-extra><a nz-button nzType="primary" routerLink="/groups">Nhóm của tôi</a></div>
      </nz-result>
    }
    @if (store.group(); as g) {
      @if (g.status !== 'LOCKED' || g.myRole !== 'LEADER') {
        <nz-result
          nzStatus="info"
          nzTitle="Không thể chọn ghế lúc này"
          nzSubTitle="Chỉ trưởng nhóm chọn ghế, khi nhóm đã chốt suất và chưa giữ ghế."
        >
          <div nz-result-extra><a nz-button nzType="primary" [routerLink]="['/groups', g.id]">Về trang nhóm</a></div>
        </nz-result>
      } @else {
        <div class="head">
          <a [routerLink]="['/groups', g.id]">← {{ g.name }}</a>
          <h1>Chọn ghế cho {{ store.ticketCount() }} người</h1>
          <div class="muted">
            {{ g.movie.title }} · {{ g.locked!.showtime.startAt | date: 'HH:mm, EEEE dd/MM/yyyy' : vn }} ·
            {{ g.locked!.showtime.cinemaName }} · {{ g.locked!.showtime.roomName }}
          </div>
        </div>

        <div class="layout">
          <nz-card nzTitle="Phương án đề xuất" class="panel">
            <form class="options" (ngSubmit)="suggest()">
              <!-- eslint-disable-next-line @angular-eslint/template/label-has-associated-control -- label[nz-checkbox] tự chứa ô input -->
              <label nz-checkbox name="allowSplit" [(ngModel)]="allowSplit">Cho phép ngồi chia hàng</label>
              <div class="field">
                <label for="seat-types">Loại ghế</label>
                <nz-select
                  id="seat-types"
                  name="seatTypes"
                  nzMode="multiple"
                  [(ngModel)]="seatTypes"
                  nzPlaceHolder="Mọi loại ghế"
                  class="select"
                >
                  @for (t of store.seatMap()?.seatTypes ?? []; track t.code) {
                    <nz-option [nzValue]="t.code" [nzLabel]="t.name" />
                  }
                </nz-select>
              </div>
              <div class="field">
                <label for="max-price">Giá tối đa / người (₫)</label>
                <nz-input-number
                  id="max-price"
                  name="maxPrice"
                  [(ngModel)]="maxPrice"
                  [nzMin]="10000"
                  [nzStep]="10000"
                  nzPlaceHolder="Theo ngân sách nhóm"
                />
              </div>
              <button nz-button nzType="primary" type="submit" [nzLoading]="store.loadingSuggestions()">Đề xuất</button>
            </form>

            @if (store.conflict(); as conflict) {
              <nz-alert nzType="warning" nzShowIcon [nzMessage]="conflict" class="alert" />
            }

            @if (store.suggestions(); as result) {
              @if (result.options.length === 0) {
                <nz-alert nzType="error" nzShowIcon [nzMessage]="reasonText(result.reason)" class="alert" />
              }
              @for (o of result.options; track o.rank) {
                <button
                  type="button"
                  class="option"
                  [class.active]="store.selectedRank() === o.rank"
                  (click)="store.choose(o)"
                >
                  <div class="option-head">
                    <strong>#{{ o.rank }} · {{ o.labels.join(', ') }}</strong>
                    <nz-tag [nzColor]="scoreColor(o.score)">{{ o.score | number: '1.0-1' }} điểm</nz-tag>
                  </div>
                  <div class="explain">{{ o.explanation }}</div>
                  <div class="muted small">{{ o.totalPrice | vnd }} · {{ o.capacity }} chỗ</div>
                </button>
              }
            } @else if (store.loadingSuggestions()) {
              <nz-spin nzSimple />
            }
          </nz-card>

          <div class="map-col">
            <nz-card nzTitle="Sơ đồ ghế" class="panel">
              <p class="muted small">
                Bấm ghế trống để thêm/bớt — thành viên trong nhóm thấy phương án bạn đang chọn theo thời gian thực.
              </p>
              @if (store.seatMap(); as map) {
                <div class="legend">
                  @for (t of map.seatTypes; track t.code) {
                    <span class="item"><i [style.background]="t.color"></i>{{ t.name }} · {{ t.price | vnd }}</span>
                  }
                  <span class="item"><i class="selected"></i>Đang chọn</span>
                  <span class="item"><i class="held"></i>Đang được giữ</span>
                  <span class="item"><i class="sold"></i>Đã bán</span>
                </div>
                <div class="grid-wrap">
                  <lib-seat-grid
                    [gridRows]="map.gridRows"
                    [gridCols]="map.gridCols"
                    [seats]="gridSeats()"
                    [colors]="colors()"
                    [cellSize]="cellSize"
                    (seatClick)="onSeat($event)"
                  />
                </div>
              } @else {
                <nz-skeleton [nzActive]="true" />
              }
            </nz-card>

            <div class="bar">
              <div>
                <div>
                  Đã chọn <strong>{{ store.selectedCapacity() }}/{{ store.ticketCount() }}</strong> chỗ
                  @if (selectedLabels()) {
                    · {{ selectedLabels() }}
                  }
                </div>
                @if (store.evaluation(); as e) {
                  <div class="small">
                    {{ e.rank === 0 ? 'Lựa chọn của bạn' : 'Phương án #' + e.rank }}: {{ e.score | number: '1.0-1' }} điểm —
                    {{ e.explanation }}
                  </div>
                }
                @if (store.evaluationError(); as error) {
                  <div class="small error">{{ error }}</div>
                }
              </div>
              <div class="bar-actions">
                <button nz-button [disabled]="store.selectedIds().length === 0" (click)="store.clearSelection()">Bỏ chọn</button>
                <button
                  nz-button
                  nzType="primary"
                  nzSize="large"
                  [disabled]="!store.selectionValid()"
                  [nzLoading]="store.busy()"
                  (click)="hold()"
                >
                  Giữ ghế &amp; thanh toán
                </button>
              </div>
            </div>
          </div>
        </div>
      }
    } @else if (!store.notFound()) {
      <nz-skeleton [nzActive]="true" [nzParagraph]="{ rows: 8 }" />
    }
  `,
  styles: `
    .head {
      margin-bottom: 16px;

      h1 {
        margin: 4px 0;
      }
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .small {
      font-size: 13px;
    }

    .error {
      color: #cf1322;
    }

    .layout {
      display: grid;
      grid-template-columns: 360px minmax(0, 1fr);
      gap: 16px;
      align-items: start;
    }

    .options {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-bottom: 16px;
    }

    .field {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .select {
      width: 100%;
    }

    .alert {
      margin-bottom: 12px;
    }

    .option {
      display: block;
      width: 100%;
      margin-bottom: 8px;
      padding: 10px 12px;
      text-align: left;
      background: #fff;
      border: 1px solid #f0f0f0;
      border-radius: 8px;
      cursor: pointer;

      &:hover {
        border-color: #91caff;
      }

      &.active {
        border-color: #1677ff;
        box-shadow: 0 0 0 2px #e6f4ff;
      }
    }

    .option-head {
      display: flex;
      justify-content: space-between;
      gap: 8px;
    }

    .explain {
      margin: 4px 0;
    }

    .grid-wrap {
      overflow-x: auto;
    }

    .legend {
      display: flex;
      flex-wrap: wrap;
      gap: 6px 16px;
      margin-bottom: 12px;
      font-size: 13px;
    }

    .item {
      display: inline-flex;
      align-items: center;
      gap: 6px;

      i {
        display: inline-block;
        width: 14px;
        height: 14px;
        border-radius: 4px 4px 2px 2px;
      }

      i.selected {
        background: #e6f4ff;
        outline: 2px solid #1677ff;
      }

      i.held {
        background: repeating-linear-gradient(45deg, #ffc069, #ffc069 3px, #fff1d6 3px, #fff1d6 6px);
      }

      i.sold {
        background: #434343;
      }
    }

    .bar {
      position: sticky;
      bottom: 0;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      margin-top: 16px;
      padding: 12px 16px;
      background: #fff;
      border-top: 3px solid #1677ff;
      border-radius: 8px;
      box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.08);
    }

    .bar-actions {
      display: flex;
      gap: 8px;
    }

    @media (max-width: 900px) {
      .layout {
        grid-template-columns: 1fr;
      }

      .bar {
        flex-direction: column;
        align-items: stretch;
      }
    }
  `,
})
export class GroupSeatsPage {
  protected readonly store = inject(GroupSeatsStore);
  private readonly router = inject(Router);

  readonly id = input.required<string>();

  protected readonly vn = VN_OFFSET;
  protected readonly cellSize = typeof window !== 'undefined' && window.innerWidth < 576 ? 22 : 28;
  protected allowSplit = true;
  protected seatTypes: string[] = [];
  protected maxPrice: number | null = null;

  private readonly suggested = signal(false);

  protected readonly colors = computed(() =>
    Object.fromEntries((this.store.seatMap()?.seatTypes ?? []).map((t) => [t.code, t.color])),
  );

  protected readonly gridSeats = computed<GridSeat[]>(() => {
    const selected = new Set(this.store.selectedIds());
    return (this.store.seatMap()?.seats ?? []).map((s) => ({
      gridRow: s.gridRow,
      gridCol: s.gridCol,
      colSpan: s.colSpan,
      seatTypeCode: s.seatTypeCode,
      label: s.rowLabel + s.number,
      state: selected.has(s.id) ? 'selected' : SEAT_STATE[s.status],
      ref: s,
    }));
  });

  protected readonly selectedLabels = computed(() => {
    const selected = new Set(this.store.selectedIds());
    return (this.store.seatMap()?.seats ?? [])
      .filter((s) => selected.has(s.id))
      .map((s) => s.rowLabel + s.number)
      .join(', ');
  });

  constructor() {
    effect(() => this.store.load(Number(this.id())));
    // Đề xuất ngay khi trang sẵn sàng
    effect(() => {
      const group = this.store.group();
      if (group?.status === 'LOCKED' && group.myRole === 'LEADER' && !this.suggested()) {
        this.suggested.set(true);
        this.suggest();
      }
    });
  }

  protected suggest(): void {
    this.store.suggest({ allowSplit: this.allowSplit, maxPricePerPerson: this.maxPrice, seatTypes: this.seatTypes });
  }

  protected onSeat(seat: GridSeat): void {
    if (seat.state === 'normal' || seat.state === 'selected') {
      this.store.toggleSeat((seat.ref as SeatMapSeat).id);
    }
  }

  protected hold(): void {
    this.store.holdAndOrder().subscribe({
      next: (code) => void this.router.navigate(['/checkout', code]),
      error: () => undefined,
    });
  }

  protected reasonText(reason: string | null): string {
    return (reason && REASONS[reason]) || 'Không có phương án phù hợp.';
  }

  protected scoreColor(score: number): string {
    return score >= 80 ? 'green' : score >= 60 ? 'gold' : 'orange';
  }
}
