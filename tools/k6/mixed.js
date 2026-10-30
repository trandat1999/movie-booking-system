// Kịch bản k6: tải hỗn hợp — 70% xem sơ đồ ghế, 20% giữ 2 ghế rồi trả, 10% giữ → tạo đơn → thanh toán (cổng giả lập).
// Cùng logic với `node tools/load/run.mjs mixed`.
//
//   k6 run -e VUS=100 -e DURATION=60s -e SHOWTIME_IDS=12,13,14,15 tools/k6/mixed.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import exec from 'k6/execution';

const API = __ENV.API_URL || 'http://localhost:8080/api/v1';
const VUS = Number(__ENV.VUS || 100);
const SHOWTIME_IDS = (__ENV.SHOWTIME_IDS || '').split(',').filter(Boolean).map(Number);
const JSON_HEADERS = { 'Content-Type': 'application/json' };

const seatmapMs = new Trend('seatmap_ms', true);
const holdMs = new Trend('hold_ms', true);
const orderMs = new Trend('order_ms', true);
const ipnMs = new Trend('ipn_ms', true);

export const options = {
  vus: VUS,
  duration: __ENV.DURATION || '60s',
  thresholds: {
    seatmap_ms: ['p(95)<200'], // PLAN.md 12: API sơ đồ ghế p95 < 200 ms
    hold_ms: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const run = Math.random().toString(36).slice(2, 8);
  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    const res = http.post(
      `${API}/auth/register`,
      JSON.stringify({ fullName: `K6 ${i}`, email: `k6m-${run}-${i}@load.local`, password: 'LoadTest@123' }),
      { headers: JSON_HEADERS },
    );
    tokens.push(res.json('data.accessToken'));
  }
  return { tokens };
}

/** Đoạn ghế trống liền nhau, toàn ghế đơn — giống freeBlocks() trong tools/load/run.mjs. */
function freeBlocks(seats) {
  const rows = {};
  seats.forEach((s) => (rows[s.gridRow] = rows[s.gridRow] || []).push(s));
  const blocks = [];
  Object.values(rows).forEach((row) => {
    row.sort((a, b) => a.gridCol - b.gridCol);
    let block = [];
    const flush = () => {
      if (block.length && block.every((s) => s.colSpan === 1)) blocks.push(block);
      block = [];
    };
    row.forEach((seat) => {
      const prev = block[block.length - 1];
      if (prev && seat.gridCol !== prev.gridCol + prev.colSpan) flush();
      if (seat.status === 'AVAILABLE') block.push(seat);
      else flush();
    });
    flush();
  });
  return blocks.filter((b) => b.length === 2 || b.length >= 4).sort((a, b) => b.length - a.length);
}

export default function (data) {
  sleep(0.5 + Math.random());
  const token = data.tokens[exec.vu.idInTest - 1];
  const auth = { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } };
  const showtimeId = SHOWTIME_IDS[Math.floor(Math.random() * SHOWTIME_IDS.length)];

  const map = http.get(`${API}/showtimes/${showtimeId}/seats`);
  seatmapMs.add(map.timings.duration);
  check(map, { 'sơ đồ ghế 200': (r) => r.status === 200 });
  const roll = Math.random();
  if (roll < 0.7 || map.status !== 200) return;

  const blocks = freeBlocks(map.json('data.seats'));
  if (!blocks.length) return;
  const b = blocks[Math.floor(Math.random() * Math.min(3, blocks.length))];
  const seatIds = (Math.random() < 0.5 ? b.slice(0, 2) : b.slice(-2)).map((s) => s.id);
  const hold = http.post(`${API}/holds`, JSON.stringify({ showtimeId, seatIds }), {
    ...auth,
    responseCallback: http.expectedStatuses(200, 409),
  });
  holdMs.add(hold.timings.duration);
  check(hold, { 'giữ ghế 200/409': (r) => r.status === 200 || r.status === 409 });
  if (hold.status !== 200) return;
  const holdId = hold.json('data.id');

  if (roll < 0.9) {
    http.del(`${API}/holds/${holdId}`, null, auth);
    return;
  }
  const order = http.post(`${API}/orders`, JSON.stringify({ holdId }), auth);
  orderMs.add(order.timings.duration);
  const code = order.json('data.code');
  const pay = http.post(`${API}/orders/${code}/pay`, JSON.stringify({ provider: 'MOCK' }), auth);
  const url = pay.json('data.redirectUrl');
  const param = (name) => decodeURIComponent((url.match(new RegExp(`[?&]${name}=([^&]*)`)) || [])[1] || '');
  const ipn = http.post(
    `${API}/payments/MOCK/ipn`,
    JSON.stringify({
      orderCode: param('orderCode'),
      transactionId: param('transactionId'),
      amount: param('amount'),
      result: 'SUCCESS',
      signature: param('sigSuccess'),
    }),
    { headers: JSON_HEADERS },
  );
  ipnMs.add(ipn.timings.duration);
  check(ipn, { 'đơn PAID': (r) => r.json('data.orderStatus') === 'PAID' });
}
