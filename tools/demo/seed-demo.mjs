#!/usr/bin/env node
/**
 * Chuẩn bị dữ liệu cho buổi demo (docs/demo-script.md): 5 tài khoản khách hàng demo1…demo5@demo.local và mã giảm
 * giá NHOM5 (giảm 10 % cho đơn từ 4 vé). Chạy lại nhiều lần không sao (tài khoản / mã đã có thì bỏ qua).
 *
 *   node tools/demo/seed-demo.mjs
 */
const API = process.env.API_URL ?? 'http://localhost:8080/api/v1';
const PASSWORD = 'Demo@1234';
const NAMES = ['An Nguyễn', 'Bình Trần', 'Chi Lê', 'Dũng Phạm', 'Em Hoàng'];

async function post(path, body, token) {
  const res = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(body),
  });
  return { status: res.status, json: await res.json().catch(() => null) };
}

for (const [i, fullName] of NAMES.entries()) {
  const email = `demo${i + 1}@demo.local`;
  const r = await post('/auth/register', { fullName, email, password: PASSWORD });
  console.log(`${email.padEnd(20)} ${r.status === 201 ? 'đã tạo' : `bỏ qua (${r.json?.error?.code ?? r.status})`}`);
}

const admin = await post('/auth/login', { identifier: 'admin@moviebooking.local', password: 'Password123' });
const token = admin.json?.data?.accessToken;
if (!token) {
  console.error('Không đăng nhập được admin — kiểm tra backend dev đang chạy');
  process.exit(1);
}
const now = new Date();
const promo = await post(
  '/admin/promotions',
  {
    code: 'NHOM5',
    name: 'Ưu đãi nhóm từ 4 vé',
    type: 'PERCENT',
    value: 10,
    maxDiscount: 100000,
    minOrder: 0,
    minTickets: 4,
    usageLimit: 100,
    perUserLimit: 1,
    startsAt: now.toISOString(),
    endsAt: new Date(now.getTime() + 30 * 86400e3).toISOString(),
    status: 'ACTIVE',
  },
  token,
);
console.log(`Mã NHOM5            ${promo.status === 201 ? 'đã tạo' : `bỏ qua (${promo.json?.error?.code ?? promo.status})`}`);
console.log(`\nMật khẩu chung các tài khoản demo: ${PASSWORD}`);
