import { SeatCell } from '@mb/shared';

/** Các hàm thuần cho trình thiết kế sơ đồ ghế; backend kiểm tra lại toàn bộ khi lưu (SeatLayoutPlanner). */

export type SpanLookup = Record<string, number>;

const key = (row: number, col: number): number => row * 1000 + col;

/** Ô (row, col) → chỉ số ghế đang chiếm ô đó. */
export function occupancy(seats: SeatCell[], spans: SpanLookup): Map<number, number> {
  const map = new Map<number, number>();
  seats.forEach((seat, index) => {
    for (let c = seat.gridCol; c < seat.gridCol + (spans[seat.seatTypeCode] ?? 1); c++) {
      map.set(key(seat.gridRow, c), index);
    }
  });
  return map;
}

/**
 * Đặt ghế loại {@link code} tại ô. Ô đang có ghế → thay loại ghế đó. Trả về null nếu không đủ chỗ (ghế đôi cần
 * ô bên phải trống và còn trong lưới).
 */
export function placeSeat(
  seats: SeatCell[],
  row: number,
  col: number,
  code: string,
  spans: SpanLookup,
  gridCols: number,
): SeatCell[] | null {
  const occupied = occupancy(seats, spans);
  const existing = occupied.get(key(row, col));
  const start = existing === undefined ? col : seats[existing].gridCol;
  const remaining = existing === undefined ? seats : seats.filter((_, i) => i !== existing);
  const span = spans[code] ?? 1;
  if (start + span > gridCols) {
    return null;
  }
  const others = occupancy(remaining, spans);
  for (let c = start; c < start + span; c++) {
    if (others.has(key(row, c))) {
      return null;
    }
  }
  return [...remaining, { gridRow: row, gridCol: start, seatTypeCode: code }];
}

export function eraseSeat(seats: SeatCell[], row: number, col: number, spans: SpanLookup): SeatCell[] {
  const index = occupancy(seats, spans).get(key(row, col));
  return index === undefined ? seats : seats.filter((_, i) => i !== index);
}

/** Bỏ các ghế không còn nằm trong lưới sau khi đổi kích thước. */
export function fitToGrid(seats: SeatCell[], rows: number, cols: number, spans: SpanLookup): SeatCell[] {
  return seats.filter((s) => s.gridRow < rows && s.gridCol + (spans[s.seatTypeCode] ?? 1) <= cols);
}

export interface QuickLayoutOptions {
  rows: number;
  seatsPerRow: number;
  /** Lối đi dọc sau ghế thứ n (đếm từ 1) */
  aisleAfter: number[];
  /** Hàng VIP (đếm từ 1, gồm cả hai đầu); 0 = không có */
  vipFrom: number;
  vipTo: number;
  coupleLastRow: boolean;
}

/** Tạo nhanh sơ đồ chữ nhật có lối đi dọc, hàng VIP ở giữa và hàng ghế đôi cuối phòng. */
export function quickLayout(options: QuickLayoutOptions): { gridRows: number; gridCols: number; seats: SeatCell[] } {
  const aisles = [...new Set(options.aisleAfter.filter((n) => n > 0 && n < options.seatsPerRow))].sort((a, b) => a - b);
  const gridCols = options.seatsPerRow + aisles.length;
  // các khối ghế liền nhau giữa các lối đi: [cột bắt đầu, cột kết thúc)
  const blocks: [number, number][] = [];
  let seatStart = 0;
  let colStart = 0;
  for (const boundary of [...aisles, options.seatsPerRow]) {
    const width = boundary - seatStart;
    blocks.push([colStart, colStart + width]);
    colStart += width + 1;
    seatStart = boundary;
  }

  const seats: SeatCell[] = [];
  for (let row = 0; row < options.rows; row++) {
    const couple = options.coupleLastRow && row === options.rows - 1;
    const vip = row + 1 >= options.vipFrom && row + 1 <= options.vipTo;
    for (const [from, to] of blocks) {
      let c = from;
      while (c < to) {
        if (couple && c + 1 < to) {
          seats.push({ gridRow: row, gridCol: c, seatTypeCode: 'COUPLE' });
          c += 2;
        } else {
          seats.push({ gridRow: row, gridCol: c, seatTypeCode: vip ? 'VIP' : 'STANDARD' });
          c++;
        }
      }
    }
  }
  return { gridRows: options.rows, gridCols, seats };
}
