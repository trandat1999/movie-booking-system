import { expect, test } from '@playwright/test';

import { findShowtime, loginUi, registerUser } from './helpers';

/** Đặt vé cá nhân: chọn ghế → giữ → thanh toán giả lập → vé QR → Vé của tôi; rồi hủy và được hoàn tiền. */
test('khách đặt vé cá nhân và nhận vé QR', async ({ page, request }) => {
  const user = await registerUser(request, 'personal');
  const pick = await findShowtime(request, 3);
  await loginUi(page, user.email);

  await page.goto(`/showtimes/${pick.showtimeId}`);
  for (const label of pick.seatLabels) {
    await page.locator(`button[aria-label^="Ghế ${label} "]`).click();
  }
  await expect(page.locator('.booking-bar')).toContainText(pick.seatLabels.join(', '));
  await page.getByRole('button', { name: 'Tiếp tục' }).click();

  await expect(page).toHaveURL(/\/checkout\/MB/);
  await expect(page.locator('app-countdown')).toBeVisible();
  await page.getByRole('button', { name: 'Thanh toán', exact: true }).click();

  await expect(page).toHaveURL(/\/payment\/mock/);
  await page.getByRole('button', { name: 'Thanh toán thành công' }).click();

  await expect(page).toHaveURL(/\/orders\/MB/);
  await expect(page.getByText('Đặt vé thành công!')).toBeVisible();
  await expect(page.locator('app-ticket-card img')).toHaveCount(2);

  await page.goto('/account/tickets');
  await expect(page.locator('app-ticket-card')).toHaveCount(2);

  // Hủy vé (suất sau 3 ngày, trong hạn) → hoàn tiền
  await page.goto('/account/orders');
  await page.locator('nz-table tbody tr a').first().click();
  await page.getByRole('button', { name: 'Hủy vé' }).click();
  await page.locator('.ant-modal-confirm').getByRole('button', { name: 'Hủy vé' }).click();
  await expect(page.locator('nz-alert', { hasText: /Đã hoàn .* về phương thức thanh toán ban đầu/ })).toBeVisible();
});
