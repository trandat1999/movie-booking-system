// Kịch bản k6: N VU cùng giữ MỘT ghế (hoặc một cụm 4 ghế chồng lấn) tại cùng thời điểm → chỉ 1 lượt giữ thành công.
// Cùng logic với tools/load/run.mjs (bản Node dùng khi máy chưa cài k6).
//
//   k6 run -e VUS=500 -e SHOWTIME_ID=12 -e SEAT_IDS=345 tools/k6/same-seat.js
//   k6 run -e VUS=300 -e SHOWTIME_ID=12 -e CLUSTER=345,346,347,348,349,350,351,352 tools/k6/same-seat.js
//
// SEAT_IDS: ghế mép dãy (giữ một mình không tạo ghế lẻ). CLUSTER: 8 ghế trống liền nhau, mỗi VU giữ 4 ghế lệch 0/2/4.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

const API = __ENV.API_URL || 'http://localhost:8080/api/v1';
const VUS = Number(__ENV.VUS || 100);
const SHOWTIME_ID = Number(__ENV.SHOWTIME_ID);
const SEAT_IDS = (__ENV.SEAT_IDS || '').split(',').filter(Boolean).map(Number);
const CLUSTER = (__ENV.CLUSTER || '').split(',').filter(Boolean).map(Number);

const holdSuccess = new Counter('hold_success');
const holdConflict = new Counter('hold_conflict');
const holdOther = new Counter('hold_other');

export const options = {
  scenarios: {
    // Mỗi VU đúng 1 lượt, tất cả bắt đầu cùng lúc
    contention: { executor: 'per-vu-iterations', vus: VUS, iterations: 1, maxDuration: '1m' },
  },
  thresholds: {
    hold_success: ['count==1'],
    http_req_failed: [{ threshold: 'rate<1', abortOnFail: false }],
  },
};

/** Đăng ký trước VUS tài khoản (register trả về access token). */
export function setup() {
  const run = Math.random().toString(36).slice(2, 8);
  const tokens = [];
  for (let i = 0; i < VUS; i++) {
    const res = http.post(
      `${API}/auth/register`,
      JSON.stringify({ fullName: `K6 ${i}`, email: `k6-${run}-${i}@load.local`, password: 'LoadTest@123' }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    tokens.push(res.json('data.accessToken'));
  }
  return { tokens };
}

export default function (data) {
  const i = exec.vu.idInTest - 1;
  const offset = [0, 2, 4][i % 3];
  const seatIds = CLUSTER.length ? CLUSTER.slice(offset, offset + 4) : SEAT_IDS;
  const res = http.post(`${API}/holds`, JSON.stringify({ showtimeId: SHOWTIME_ID, seatIds }), {
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${data.tokens[i]}` },
    responseCallback: http.expectedStatuses(200, 409),
  });
  if (res.status === 200) holdSuccess.add(1);
  else if (res.status === 409) holdConflict.add(1);
  else holdOther.add(1);
  check(res, { 'giữ thành công hoặc 409': (r) => r.status === 200 || r.status === 409 });
}
