import { Component, computed, input, output } from '@angular/core';

/** Trạng thái hiển thị của một ghế trên sơ đồ. */
export type GridSeatState = 'normal' | 'inactive' | 'held' | 'sold' | 'selected';

export interface GridSeat {
  gridRow: number;
  gridCol: number;
  colSpan: number;
  seatTypeCode: string;
  /** Ví dụ "E7"; trống khi đang thiết kế chưa lưu */
  label?: string | null;
  state?: GridSeatState;
  /** Dữ liệu tùy ý của nơi dùng (id ghế, ...) */
  ref?: unknown;
}

export interface GridCell {
  gridRow: number;
  gridCol: number;
}

/**
 * Sơ đồ phòng chiếu dạng lưới (presentational). Dùng chung cho trình thiết kế sơ đồ (admin) và chọn ghế (web).
 * Ô trống là lối đi; chỉ hiển thị/bấm được khi {@link editable}.
 */
@Component({
  selector: 'lib-seat-grid',
  template: `
    <div class="wrapper">
      <div class="screen">MÀN HÌNH</div>
      <div class="grid" [style.grid-template-columns]="columns()" [style.grid-template-rows]="rows()">
        @for (label of rowLabels(); track $index) {
          @if (label) {
            <div class="row-label" [style.grid-row]="$index + 1" [style.grid-column]="1">{{ label }}</div>
            <div class="row-label" [style.grid-row]="$index + 1" [style.grid-column]="gridCols() + 2">{{ label }}</div>
          }
        }
        @if (editable()) {
          @for (cell of emptyCells(); track cell.gridRow * 1000 + cell.gridCol) {
            <button
              type="button"
              class="cell"
              [style.grid-row]="cell.gridRow + 1"
              [style.grid-column]="cell.gridCol + 2"
              [attr.aria-label]="'Ô hàng ' + (cell.gridRow + 1) + ' cột ' + (cell.gridCol + 1)"
              (click)="cellClick.emit(cell)"
            ></button>
          }
        }
        @for (seat of seats(); track seat.gridRow * 1000 + seat.gridCol) {
          <button
            type="button"
            class="seat"
            [class]="'seat state-' + (seat.state ?? 'normal')"
            [style.grid-row]="seat.gridRow + 1"
            [style.grid-column]="seat.gridCol + 2 + ' / span ' + seat.colSpan"
            [style.--seat-color]="colorOf(seat.seatTypeCode)"
            [attr.title]="seat.label ?? ''"
            [attr.aria-label]="'Ghế ' + (seat.label ?? '') + ' ' + seat.seatTypeCode"
            (click)="seatClick.emit(seat)"
          >
            {{ showLabels() ? seat.label : '' }}
          </button>
        }
      </div>
    </div>
  `,
  styles: `
    :host {
      display: block;
      overflow-x: auto;
    }

    .wrapper {
      display: inline-flex;
      flex-direction: column;
      align-items: center;
      min-width: 100%;
      padding: 8px;
    }

    .screen {
      width: 70%;
      margin-bottom: 24px;
      padding: 4px 0;
      border-top: 4px solid #bfbfbf;
      border-radius: 50% 50% 0 0 / 12px 12px 0 0;
      color: #8c8c8c;
      font-size: 12px;
      letter-spacing: 4px;
      text-align: center;
    }

    .grid {
      display: grid;
      gap: 4px;
    }

    .row-label {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #8c8c8c;
      font-size: 12px;
      font-weight: 600;
    }

    .cell,
    .seat {
      padding: 0;
      border: none;
      border-radius: 6px 6px 3px 3px;
      font-size: 10px;
      cursor: pointer;
    }

    .cell {
      border: 1px dashed #d9d9d9;
      background: transparent;
    }

    .cell:hover {
      background: #f0f0f0;
    }

    .seat {
      background: var(--seat-color);
      color: #fff;
      font-weight: 600;
    }

    .seat:hover {
      filter: brightness(1.1);
    }

    .state-inactive {
      background: repeating-linear-gradient(45deg, #d9d9d9, #d9d9d9 4px, #f5f5f5 4px, #f5f5f5 8px);
      color: #8c8c8c;
    }

    .state-held {
      background: #faad14;
    }

    .state-sold {
      background: #434343;
      cursor: not-allowed;
    }

    .state-selected {
      outline: 3px solid #1677ff;
      outline-offset: 1px;
    }
  `,
})
export class SeatGridComponent {
  readonly gridRows = input.required<number>();
  readonly gridCols = input.required<number>();
  readonly seats = input.required<GridSeat[]>();
  /** Mã loại ghế → màu */
  readonly colors = input<Record<string, string>>({});
  readonly editable = input(false);
  readonly showLabels = input(true);
  readonly cellSize = input(28);

  readonly cellClick = output<GridCell>();
  readonly seatClick = output<GridSeat>();

  protected colorOf(code: string): string {
    return this.colors()[code] || '#8c8c8c';
  }

  protected readonly columns = computed(() => `24px repeat(${this.gridCols()}, ${this.cellSize()}px) 24px`);
  protected readonly rows = computed(() => `repeat(${this.gridRows()}, ${this.cellSize()}px)`);

  /** Nhãn hàng lấy từ ghế đầu tiên có nhãn trong hàng. */
  protected readonly rowLabels = computed(() => {
    const labels: (string | null)[] = Array.from({ length: this.gridRows() }, () => null);
    for (const seat of this.seats()) {
      if (seat.label && !labels[seat.gridRow]) {
        labels[seat.gridRow] = seat.label.replace(/\d+$/, '');
      }
    }
    return labels;
  });

  protected readonly emptyCells = computed(() => {
    const occupied = new Set<number>();
    for (const seat of this.seats()) {
      for (let c = seat.gridCol; c < seat.gridCol + seat.colSpan; c++) {
        occupied.add(seat.gridRow * 1000 + c);
      }
    }
    const cells: GridCell[] = [];
    for (let r = 0; r < this.gridRows(); r++) {
      for (let c = 0; c < this.gridCols(); c++) {
        if (!occupied.has(r * 1000 + c)) {
          cells.push({ gridRow: r, gridCol: c });
        }
      }
    }
    return cells;
  });
}
