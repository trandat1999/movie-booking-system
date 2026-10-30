import { createHmac, randomUUID } from 'node:crypto';

import { APIRequestContext, expect, Page } from '@playwright/test';

export const API = process.env['API_URL'] ?? 'http://localhost:8080/api/v1';
export const ADMIN_URL = process.env['ADMIN_URL'] ?? 'http://localhost:4300';
export const PASSWORD = 'E2eTest@123';
/** Khóa ký QR của môi trường dev (application-dev.yml) — dùng để tạo nội dung QR như khi quét ảnh. */
const DEV_TICKET_SECRET = process.env['TICKET_SECRET'] ?? 'dev-only-ticket-secret';

export interface TestUser {
  email: string;
  fullName: string;
}

interface SeatMapSeat {
  id: number;
  rowLabel: string;
  number: number;
  gridRow: number;
  gridCol: number;
  colSpan: number;
  seatTypeCode: string;
  status: string;
}

export interface ShowtimePick {
  showtimeId: number;
  movieId: number;
  startAt: string;
  /** dd/MM theo giờ VN — nhãn trên thanh chọn ngày */
  dayMonth: string;
  /** 2 ghế liền nhau ở mép một dãy trống dài ≥ 4 ghế (chọn không tạo ghế lẻ) */
  seatLabels: string[];
  seatIds: number[];
}

export async function registerUser(request: APIRequestContext, prefix: string): Promise<TestUser> {
  const user = { email: `${prefix}-${randomUUID().slice(0, 8)}@e2e.local`, fullName: `E2E ${prefix}` };
  const response = await request.post(`${API}/auth/register`, {
    data: { fullName: user.fullName, email: user.email, password: PASSWORD },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  return user;
}

export async function apiLogin(request: APIRequestContext, email: string, password = PASSWORD): Promise<string> {
  const response = await request.post(`${API}/auth/login`, { data: { identifier: email, password } });
  expect(response.ok(), await response.text()).toBeTruthy();
  return (await response.json()).data.accessToken as string;
}

export async function loginUi(page: Page, email: string, password = PASSWORD, baseUrl = ''): Promise<void> {
  await page.goto(`${baseUrl}/login`);
  await page.locator('#identifier').fill(email);
  await page.locator('#password').fill(password);
  await page.locator('button[type="submit"]').click();
  await expect(page).not.toHaveURL(/\/login/);
}

function vnDate(daysAhead: number): string {
  return new Date(Date.now() + 7 * 3600e3 + daysAhead * 86400e3).toISOString().slice(0, 10);
}

/** Tìm suất còn bán trong ngày (giờ VN) có một dãy ghế thường trống liền nhau ≥ 4 ghế. */
export async function findShowtime(
  request: APIRequestContext,
  daysAhead: number,
  filter: (s: { startAt: string; availableSeats: number }) => boolean = () => true,
): Promise<ShowtimePick> {
  const date = vnDate(daysAhead);
  const showtimes = (await (await request.get(`${API}/showtimes?date=${date}`)).json()).data as {
    id: number;
    movieId: number;
    startAt: string;
    availableSeats: number;
  }[];
  for (const s of showtimes.filter(filter)) {
    const map = (await (await request.get(`${API}/showtimes/${s.id}/seats`)).json()).data as { seats: SeatMapSeat[] };
    const run = freeRun(map.seats);
    if (run) {
      return {
        showtimeId: s.id,
        movieId: s.movieId,
        startAt: s.startAt,
        dayMonth: `${date.slice(8, 10)}/${date.slice(5, 7)}`,
        seatLabels: run.map((x) => x.rowLabel + x.number),
        seatIds: run.map((x) => x.id),
      };
    }
  }
  throw new Error(`Không tìm thấy suất phù hợp ngày ${date}`);
}

/** Hai ghế đầu của một dãy (giới hạn bởi lối đi/mép) toàn ghế STANDARD trống, dài ≥ 4. */
function freeRun(seats: SeatMapSeat[]): SeatMapSeat[] | null {
  const rows = new Map<number, SeatMapSeat[]>();
  seats.forEach((s) => rows.set(s.gridRow, [...(rows.get(s.gridRow) ?? []), s]));
  for (const row of rows.values()) {
    row.sort((a, b) => a.gridCol - b.gridCol);
    let block: SeatMapSeat[] = [];
    const flush = (): SeatMapSeat[] | null =>
      block.length >= 4 && block.every((s) => s.status === 'AVAILABLE' && s.seatTypeCode === 'STANDARD')
        ? block.slice(0, 2)
        : null;
    for (const seat of row) {
      const prev = block[block.length - 1];
      if (prev && seat.gridCol !== prev.gridCol + prev.colSpan) {
        const found = flush();
        if (found) {
          return found;
        }
        block = [];
      }
      block.push(seat);
    }
    const found = flush();
    if (found) {
      return found;
    }
  }
  return null;
}

/** Nội dung QR đã ký: mã + "." + base64url(16 byte đầu HMAC-SHA256) — giống TicketSigner. */
export function signQr(code: string): string {
  const signature = createHmac('sha256', DEV_TICKET_SECRET).update(code).digest().subarray(0, 16);
  return `${code}.${signature.toString('base64url')}`;
}

/** Đặt + thanh toán (cổng giả lập) một đơn qua API; trả về mã đơn và mã vé. */
export async function bookViaApi(
  request: APIRequestContext,
  token: string,
  pick: ShowtimePick,
): Promise<{ orderCode: string; ticketCodes: string[] }> {
  const headers = { Authorization: `Bearer ${token}` };
  const hold = await (await request.post(`${API}/holds`, { headers, data: { showtimeId: pick.showtimeId, seatIds: pick.seatIds } })).json();
  expect(hold.success, JSON.stringify(hold)).toBeTruthy();
  const order = (await (await request.post(`${API}/orders`, { headers, data: { holdId: hold.data.id } })).json()).data;
  const pay = (await (await request.post(`${API}/orders/${order.code}/pay`, { headers, data: { provider: 'MOCK' } })).json()).data;
  const params = new URL(pay.redirectUrl, 'http://x').searchParams;
  const ipn = await request.post(`${API}/payments/MOCK/ipn`, {
    data: {
      orderCode: params.get('orderCode'),
      transactionId: params.get('transactionId'),
      amount: params.get('amount'),
      result: 'SUCCESS',
      signature: params.get('sigSuccess'),
    },
  });
  expect((await ipn.json()).data.orderStatus).toBe('PAID');
  const detail = (await (await request.get(`${API}/orders/${order.code}`, { headers })).json()).data;
  return { orderCode: order.code, ticketCodes: detail.tickets.map((t: { code: string }) => t.code) };
}
