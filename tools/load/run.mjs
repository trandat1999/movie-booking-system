#!/usr/bin/env node
/**
 * Kiểm thử tải & đồng thời (PLAN.md M9) — bản chạy bằng Node 22 (fetch tích hợp), cùng kịch bản với tools/k6/*.js
 * để chạy được trên máy không cài k6. Chạy trên backend dev (dữ liệu mẫu):
 *
 *   node tools/load/run.mjs same-seat    [--vus 100,300,500]
 *   node tools/load/run.mjs same-cluster [--vus 100,300,500]
 *   node tools/load/run.mjs seatmap      [--vus 50] [--duration 30]
 *   node tools/load/run.mjs mixed        [--vus 50] [--duration 60]
 *
 * Kết quả ghi vào docs/experiments/load/ (CSV + SVG).
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';

const API = process.env.API_URL ?? 'http://localhost:8080/api/v1';
const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', '..', 'docs', 'experiments', 'load');
const PASSWORD = 'LoadTest@123';

const args = process.argv.slice(2);
const scenario = args[0];
const opt = (name, def) => {
  const i = args.indexOf(`--${name}`);
  return i >= 0 ? args[i + 1] : def;
};

// ---------- HTTP ----------
async function call(method, path, { token, body } = {}) {
  const started = performance.now();
  let status = 0;
  let json = null;
  try {
    const res = await fetch(API + path, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
    status = res.status;
    const text = await res.text();
    json = text ? JSON.parse(text) : null;
  } catch (e) {
    json = { error: { code: 'NETWORK', message: String(e) } };
  }
  return { status, json, ms: performance.now() - started };
}

async function pool(items, concurrency, fn) {
  const results = new Array(items.length);
  let next = 0;
  await Promise.all(
    Array.from({ length: Math.min(concurrency, items.length) }, async () => {
      while (next < items.length) {
        const i = next++;
        results[i] = await fn(items[i], i);
      }
    }),
  );
  return results;
}

async function registerUsers(n) {
  process.stdout.write(`Đăng ký ${n} tài khoản thử tải... `);
  const run = randomUUID().slice(0, 6);
  const tokens = await pool([...Array(n).keys()], 16, async (i) => {
    const r = await call('POST', '/auth/register', {
      body: { fullName: `Load ${i}`, email: `load-${run}-${i}@load.local`, password: PASSWORD },
    });
    if (r.status !== 201) throw new Error(`register ${r.status} ${JSON.stringify(r.json)}`);
    return r.json.data.accessToken;
  });
  console.log('xong');
  return tokens;
}

// ---------- dữ liệu ----------
function vnDate(daysAhead) {
  return new Date(Date.now() + 7 * 3600e3 + daysAhead * 86400e3).toISOString().slice(0, 10);
}

async function showtimesOn(daysAhead) {
  return (await call('GET', `/showtimes?date=${vnDate(daysAhead)}`)).json.data;
}

async function seatMap(showtimeId) {
  return (await call('GET', `/showtimes/${showtimeId}/seats`)).json.data.seats;
}

/**
 * Các đoạn ghế trống liền nhau (cắt bởi lối đi/mép/ghế đã có người), dài nhất trước. Mọi loại ghế đều tính vì
 * quy tắc chống ghế lẻ xét mọi ghế trống; chỉ giữ đoạn có toàn ghế đơn (không có ghế đôi).
 */
function freeBlocks(seats) {
  const rows = new Map();
  for (const s of seats) rows.set(s.gridRow, [...(rows.get(s.gridRow) ?? []), s]);
  const blocks = [];
  for (const row of rows.values()) {
    row.sort((a, b) => a.gridCol - b.gridCol);
    let block = [];
    const flush = () => {
      if (block.length && block.every((s) => s.colSpan === 1)) blocks.push(block);
      block = [];
    };
    for (const seat of row) {
      const prev = block[block.length - 1];
      if (prev && seat.gridCol !== prev.gridCol + prev.colSpan) flush();
      if (seat.status === 'AVAILABLE') block.push(seat);
      else flush();
    }
    flush();
  }
  return blocks.sort((a, b) => b.length - a.length);
}

async function pickShowtime(daysAhead, minBlock, accept = () => true) {
  for (const s of await showtimesOn(daysAhead)) {
    const blocks = freeBlocks(await seatMap(s.id)).filter((b) => b.length >= minBlock && accept(b));
    if (blocks.length) return { showtime: s, block: blocks[0] };
  }
  throw new Error(`Không có suất đủ ghế trống ngày ${vnDate(daysAhead)}`);
}

// ---------- thống kê ----------
function percentile(sorted, p) {
  if (!sorted.length) return 0;
  return sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];
}

function summarize(samples) {
  const s = [...samples].sort((a, b) => a - b);
  const avg = s.reduce((a, b) => a + b, 0) / (s.length || 1);
  return { count: s.length, avg, p50: percentile(s, 50), p95: percentile(s, 95), p99: percentile(s, 99), max: s.at(-1) ?? 0 };
}

const fmt = (x) => x.toFixed(1);

function writeCsv(name, header, rows) {
  mkdirSync(OUT, { recursive: true });
  writeFileSync(join(OUT, name), [header.join(','), ...rows.map((r) => r.join(','))].join('\n') + '\n');
  console.log(`→ docs/experiments/load/${name}`);
}

/** Biểu đồ cột nhóm đơn giản (SVG thuần, giống seat-bench SvgBarChart). */
function barChart(file, title, categories, series, unit) {
  const W = 720, H = 360, L = 60, B = 50, T = 40, R = 20;
  const max = Math.max(1, ...series.flatMap((s) => s.values)) * 1.15;
  const colors = ['#2563eb', '#f59e0b', '#10b981', '#ef4444'];
  const plotW = W - L - R, plotH = H - T - B;
  const groupW = plotW / categories.length;
  const barW = (groupW * 0.7) / series.length;
  let svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${W} ${H}" font-family="sans-serif" font-size="12">`;
  svg += `<rect width="${W}" height="${H}" fill="#fff"/><text x="${W / 2}" y="22" text-anchor="middle" font-size="15" font-weight="bold">${title}</text>`;
  for (let i = 0; i <= 5; i++) {
    const v = (max / 5) * i, y = T + plotH - (plotH * i) / 5;
    svg += `<line x1="${L}" x2="${W - R}" y1="${y}" y2="${y}" stroke="#e5e7eb"/><text x="${L - 6}" y="${y + 4}" text-anchor="end">${v.toFixed(v < 10 ? 1 : 0)}</text>`;
  }
  svg += `<text x="14" y="${T + plotH / 2}" transform="rotate(-90 14 ${T + plotH / 2})" text-anchor="middle">${unit}</text>`;
  categories.forEach((c, ci) => {
    const gx = L + ci * groupW + groupW * 0.15;
    series.forEach((s, si) => {
      const v = s.values[ci], h = (plotH * v) / max, x = gx + si * barW, y = T + plotH - h;
      svg += `<rect x="${x}" y="${y}" width="${barW - 2}" height="${h}" fill="${colors[si % colors.length]}"/>`;
      svg += `<text x="${x + barW / 2 - 1}" y="${y - 4}" text-anchor="middle" font-size="10">${v.toFixed(v < 10 ? 1 : 0)}</text>`;
    });
    svg += `<text x="${L + ci * groupW + groupW / 2}" y="${H - B + 18}" text-anchor="middle">${c}</text>`;
  });
  series.forEach((s, si) => {
    const x = L + si * 150;
    svg += `<rect x="${x}" y="${H - 18}" width="12" height="12" fill="${colors[si % colors.length]}"/><text x="${x + 18}" y="${H - 8}">${s.name}</text>`;
  });
  writeFileSync(join(OUT, file), svg + '</svg>\n');
  console.log(`→ docs/experiments/load/${file}`);
}

// ---------- kịch bản 1 & 2: tranh ghế ----------
/**
 * Mỗi vòng: N người dùng cùng gửi yêu cầu giữ ghế tại cùng một thời điểm.
 *  - same-seat   : tất cả cùng 1 ghế
 *  - same-cluster: cụm 4 ghế, người dùng chia đều 3 vị trí chồng lấn nhau (lệch 0/2/4 ghế trong một dãy 8 ghế)
 * Bất biến cần kiểm: không ghế nào nằm trong 2 lượt giữ thành công; số ghế HELD trên sơ đồ = tổng ghế đã giữ.
 */
async function contention(mode) {
  const vusList = opt('vus', '100,300,500').split(',').map(Number);
  const tokens = await registerUsers(Math.max(...vusList));
  const rows = [];
  let day = 3; // dữ liệu mẫu có lịch chiếu 6 ngày tới
  for (const vus of vusList) {
    const { showtime, block } = await pickShowtime(day++, mode === 'same-seat' ? 3 : 8, (b) => b.length !== 9); // dãy 9 ghế: lệch 4 để lại 1 ghế lẻ
    const requests = Array.from({ length: vus }, (_, i) => {
      // Ghế mép dãy: giữ một mình không tạo ghế lẻ (BR chống ghế lẻ)
      if (mode === 'same-seat') return [block[0].id];
      const offset = [0, 2, 4][i % 3];
      return block.slice(offset, offset + 4).map((s) => s.id);
    });
    // Rào chắn: chuẩn bị xong mọi lời gọi rồi phát cùng lúc
    const started = performance.now();
    const results = await Promise.all(
      requests.map((seatIds, i) =>
        call('POST', '/holds', { token: tokens[i], body: { showtimeId: showtime.id, seatIds } }).then((r) => ({ ...r, seatIds })),
      ),
    );
    const wall = performance.now() - started;
    const ok = results.filter((r) => r.status === 200 && r.json?.success);
    const conflict = results.filter((r) => r.status === 409);
    const other = results.filter((r) => !(r.status === 200 && r.json?.success) && r.status !== 409);
    const heldSeats = ok.flatMap((r) => r.json.data.seats.map((s) => s.seatId));
    const duplicates = heldSeats.length - new Set(heldSeats).size;
    const after = await seatMap(showtime.id);
    const heldOnMap = after.filter((s) => block.some((b) => b.id === s.id) && s.status === 'HELD').length;
    const lat = summarize(results.map((r) => r.ms));
    const otherCodes = [...new Set(other.map((r) => `${r.status}:${r.json?.error?.code}`))].join(' ');
    console.log(
      `${mode} VU=${vus}: thành công=${ok.length} xung đột(409)=${conflict.length} khác=${other.length} ${otherCodes} ` +
        `ghế trùng=${duplicates} HELD trên sơ đồ=${heldOnMap}/${heldSeats.length} p95=${fmt(lat.p95)}ms tổng=${fmt(wall)}ms`,
    );
    rows.push([vus, ok.length, conflict.length, other.length, duplicates, heldOnMap === heldSeats.length, fmt(lat.p50), fmt(lat.p95), fmt(lat.max), fmt(wall)]);
    // Trả ghế cho lần chạy sau
    await Promise.all(ok.map((r) => call('DELETE', `/holds/${r.json.data.id}`, { token: tokens[results.indexOf(r)] })));
  }
  writeCsv(`${mode}.csv`, ['vus', 'success', 'conflict_409', 'other_error', 'duplicate_seats', 'map_consistent', 'p50_ms', 'p95_ms', 'max_ms', 'wall_ms'], rows);
  barChart(
    `${mode}-latency.svg`,
    `${mode === 'same-seat' ? 'Cùng 1 ghế' : 'Cụm 4 ghế chồng lấn'}: độ trễ giữ ghế theo số VU`,
    rows.map((r) => `${r[0]} VU`),
    [
      { name: 'p50 (ms)', values: rows.map((r) => Number(r[6])) },
      { name: 'p95 (ms)', values: rows.map((r) => Number(r[7])) },
    ],
    'ms',
  );
}

// ---------- kịch bản 3: chỉ xem sơ đồ ghế ----------
async function runFor(vus, seconds, step) {
  const deadline = Date.now() + seconds * 1000;
  await Promise.all(
    Array.from({ length: vus }, async (_, vu) => {
      while (Date.now() < deadline) await step(vu);
    }),
  );
}

async function seatmapOnly() {
  const vus = Number(opt('vus', '50'));
  const duration = Number(opt('duration', '30'));
  const ids = (await showtimesOn(1)).map((s) => s.id);
  const samples = [];
  let errors = 0;
  await runFor(vus, duration, async () => {
    const r = await call('GET', `/showtimes/${ids[Math.floor(Math.random() * ids.length)]}/seats`);
    if (r.status !== 200) errors++;
    samples.push(r.ms);
  });
  const s = summarize(samples);
  console.log(`seatmap VU=${vus} ${duration}s: ${s.count} req (${fmt(s.count / duration)}/s) lỗi=${errors} p50=${fmt(s.p50)} p95=${fmt(s.p95)} p99=${fmt(s.p99)}ms`);
  writeCsv('seatmap.csv', ['vus', 'duration_s', 'requests', 'rps', 'errors', 'avg_ms', 'p50_ms', 'p95_ms', 'p99_ms', 'max_ms'], [
    [vus, duration, s.count, fmt(s.count / duration), errors, fmt(s.avg), fmt(s.p50), fmt(s.p95), fmt(s.p99), fmt(s.max)],
  ]);
}

// ---------- kịch bản 4: tải hỗn hợp ----------
/**
 * Mỗi VU lặp (nghỉ 0,5–1,5 s giữa các lượt): 70% xem sơ đồ ghế; 20% giữ 2 ghế rồi trả; 10% giữ → tạo đơn → thanh toán (cổng giả lập, IPN).
 * Ghế chọn ở mép 3 đoạn trống dài nhất của 8 suất chung (còn nhiều ghế nhất) → nhiều VU nhắm cùng ghế, có tranh chấp thật.
 * Sau cùng kiểm: không ghế nào thuộc 2 đơn PAID.
 */
async function mixed() {
  const vus = Number(opt('vus', '50'));
  const duration = Number(opt('duration', '60'));
  const tokens = await registerUsers(vus);
  // 8 suất còn nhiều ghế trống nhất trong 5 ngày tới
  const all = (await Promise.all([1, 2, 3, 4, 5].map(showtimesOn))).flat();
  const showtimes = all.sort((a, b) => b.availableSeats - a.availableSeats).slice(0, 8);
  const lat = { seatmap: [], hold: [], release: [], order: [], pay: [], ipn: [] };
  const outcome = { noSeats: 0, holdOk: 0, holdConflict: 0, holdOther: 0, paid: 0, errors: 0 };
  const paidSeats = [];
  const holdErrors = {};

  await runFor(vus, duration, async (vu) => {
    // Thời gian suy nghĩ 0,5–1,5 s như người dùng thật (cũng giữ dưới giới hạn 20 lượt giữ/phút/người — BR15)
    await new Promise((r) => setTimeout(r, 500 + Math.random() * 1000));
    const st = showtimes[Math.floor(Math.random() * showtimes.length)];
    const map = await call('GET', `/showtimes/${st.id}/seats`);
    lat.seatmap.push(map.ms);
    if (map.status !== 200) return void outcome.errors++;
    const roll = Math.random();
    if (roll < 0.7) return;
    // Đoạn 2 ghế (lấy trọn) hoặc ≥ 4 ghế (lấy 2 ghế mép) — không tạo ghế lẻ
    const blocks = freeBlocks(map.json.data.seats).filter((b) => b.length === 2 || b.length >= 4);
    if (!blocks.length) return void outcome.noSeats++;
    const b = blocks[Math.floor(Math.random() * Math.min(3, blocks.length))];
    const seatIds = (Math.random() < 0.5 ? b.slice(0, 2) : b.slice(-2)).map((s) => s.id);
    const hold = await call('POST', '/holds', { token: tokens[vu], body: { showtimeId: st.id, seatIds } });
    lat.hold.push(hold.ms);
    if (hold.status === 409) return void outcome.holdConflict++;
    if (!(hold.status === 200 && hold.json?.success)) {
      const key = `${hold.status}:${hold.json?.error?.code}`;
      holdErrors[key] = (holdErrors[key] ?? 0) + 1;
      return void outcome.holdOther++;
    }
    outcome.holdOk++;
    const holdId = hold.json.data.id;
    if (roll < 0.9) {
      const rel = await call('DELETE', `/holds/${holdId}`, { token: tokens[vu] });
      lat.release.push(rel.ms);
      return;
    }
    const order = await call('POST', '/orders', { token: tokens[vu], body: { holdId } });
    lat.order.push(order.ms);
    if (!order.json?.success) return void outcome.errors++;
    const code = order.json.data.code;
    const pay = await call('POST', `/orders/${code}/pay`, { token: tokens[vu], body: { provider: 'MOCK' } });
    lat.pay.push(pay.ms);
    if (!pay.json?.success) return void outcome.errors++;
    const p = new URL(pay.json.data.redirectUrl, 'http://x').searchParams;
    const ipn = await call('POST', '/payments/MOCK/ipn', {
      body: {
        orderCode: p.get('orderCode'),
        transactionId: p.get('transactionId'),
        amount: p.get('amount'),
        result: 'SUCCESS',
        signature: p.get('sigSuccess'),
      },
    });
    lat.ipn.push(ipn.ms);
    if (ipn.json?.data?.orderStatus === 'PAID') {
      outcome.paid++;
      paidSeats.push(...seatIds.map((id) => `${st.id}:${id}`));
    } else outcome.errors++;
  });

  const duplicates = paidSeats.length - new Set(paidSeats).size;
  const rows = Object.entries(lat).map(([op, samples]) => {
    const s = summarize(samples);
    console.log(`  ${op.padEnd(8)} n=${String(s.count).padStart(6)} p50=${fmt(s.p50)} p95=${fmt(s.p95)} p99=${fmt(s.p99)} max=${fmt(s.max)} ms`);
    return [op, s.count, fmt(s.avg), fmt(s.p50), fmt(s.p95), fmt(s.p99), fmt(s.max)];
  });
  const total = Object.values(lat).reduce((a, s) => a + s.length, 0);
  console.log(
    `mixed VU=${vus} ${duration}s: ${total} req (${fmt(total / duration)}/s) hết ghế=${outcome.noSeats} giữ OK=${outcome.holdOk} 409=${outcome.holdConflict} ` +
      `khác=${outcome.holdOther} PAID=${outcome.paid} lỗi=${outcome.errors} ghế bán trùng=${duplicates}`,
  );
  if (outcome.holdOther) console.log(`  lỗi giữ ghế khác: ${JSON.stringify(holdErrors)}`);
  writeCsv('mixed.csv', ['operation', 'count', 'avg_ms', 'p50_ms', 'p95_ms', 'p99_ms', 'max_ms'], rows);
  writeCsv('mixed-summary.csv', ['vus', 'duration_s', 'requests', 'rps', 'hold_ok', 'hold_409', 'hold_other', 'paid', 'errors', 'duplicate_sold_seats'], [
    [vus, duration, total, fmt(total / duration), outcome.holdOk, outcome.holdConflict, outcome.holdOther, outcome.paid, outcome.errors, duplicates],
  ]);
  barChart(
    'mixed-latency.svg',
    `Tải hỗn hợp ${vus} VU / ${duration}s: độ trễ theo thao tác`,
    rows.map((r) => r[0]),
    [
      { name: 'p50 (ms)', values: rows.map((r) => Number(r[3])) },
      { name: 'p95 (ms)', values: rows.map((r) => Number(r[4])) },
    ],
    'ms',
  );
}

const scenarios = { 'same-seat': () => contention('same-seat'), 'same-cluster': () => contention('same-cluster'), seatmap: seatmapOnly, mixed };
if (!scenarios[scenario]) {
  console.error(`Kịch bản: ${Object.keys(scenarios).join(' | ')}`);
  process.exit(1);
}
await scenarios[scenario]();
