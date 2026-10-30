import { expect, test } from '@playwright/test';

import { ADMIN_URL, apiLogin, bookViaApi, findShowtime, loginUi, registerUser, signQr } from './helpers';

/** Soát vé (CHK01): nhân viên quét QR vé → hợp lệ; quét lại → bị từ chối; QR cả đơn soát vé còn lại. */
test('nhân viên soát vé, quét lần hai bị từ chối', async ({ page, request }) => {
  // Cần suất chiếu trong khung soát vé (mở trước 2 giờ) — dữ liệu mẫu có suất mỗi vài giờ
  const now = Date.now();
  let pick;
  try {
    pick = await findShowtime(request, 0, (s) => {
      const start = new Date(s.startAt).getTime();
      return start > now + 5 * 60e3 && start < now + 2 * 3600e3;
    });
  } catch {
    test.skip(true, 'Không có suất nào bắt đầu trong 2 giờ tới để soát vé');
    return;
  }
  const customer = await registerUser(request, 'checkin');
  const { orderCode, ticketCodes } = await bookViaApi(request, await apiLogin(request, customer.email), pick);

  await loginUi(page, 'staff@moviebooking.local', 'Password123', ADMIN_URL);
  await page.goto(`${ADMIN_URL}/checkin`);
  const input = page.locator('#payload');
  const submit = page.locator('form.manual button[type="submit"]');

  await input.fill(signQr(ticketCodes[0]));
  await submit.click();
  await expect(page.locator('.result.ok')).toContainText('đã soát 1 vé');

  await page.waitForTimeout(4200); // khung chống quét lặp của trang
  await input.fill(signQr(ticketCodes[0]));
  await submit.click();
  await expect(page.locator('.result.fail')).toContainText('đã được soát');

  await input.fill(signQr(orderCode));
  await submit.click();
  await expect(page.locator('.result.ok')).toContainText(`đã soát ${ticketCodes.length - 1} vé`);

  await input.fill(`${ticketCodes[1]}.chu-ky-gia`);
  await submit.click();
  await expect(page.locator('.result.fail')).toContainText('không hợp lệ');
});
