import { Browser, expect, Page, test } from '@playwright/test';

import { findShowtime, loginUi, registerUser, TestUser } from './helpers';

async function userPage(browser: Browser, user: TestUser): Promise<Page> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await loginUi(page, user.email);
  return page;
}

/**
 * Đặt vé nhóm 3 người (PLAN.md 12): trưởng nhóm tạo nhóm → 2 thành viên tham gia bằng link mời và phản hồi →
 * đề xuất & chốt suất → hệ thống đề xuất cụm ghế → giữ ghế → thanh toán → mỗi người thấy vé của mình.
 */
test('nhóm 3 người đặt vé và mỗi người nhận vé', async ({ browser, request }) => {
  const pick = await findShowtime(request, 1);
  const [leaderUser, aliceUser, bobUser] = await Promise.all([
    registerUser(request, 'leader'),
    registerUser(request, 'alice'),
    registerUser(request, 'bob'),
  ]);
  const leader = await userPage(browser, leaderUser);
  const alice = await userPage(browser, aliceUser);
  const bob = await userPage(browser, bobUser);

  // Trưởng nhóm tạo nhóm từ trang phim
  await leader.goto(`/groups/new?movieId=${pick.movieId}`);
  await leader.locator('#name').fill('Nhom E2E');
  await leader.getByRole('button', { name: 'Tạo nhóm' }).click();
  await expect(leader).toHaveURL(/\/groups\/\d+$/);
  const groupUrl = leader.url();
  const code = (await leader.locator('code.code').innerText()).trim();

  // Hai thành viên tham gia qua link mời và phản hồi "Tham gia"
  for (const member of [alice, bob]) {
    await member.goto(`/groups/join/${code}`);
    await member.getByRole('button', { name: 'Tham gia nhóm' }).click();
    await expect(member).toHaveURL(/\/groups\/\d+$/);
    await member.locator('label[nz-radio-button][nzvalue="YES"]').click();
    await member.getByRole('button', { name: 'Lưu phản hồi' }).click();
    await expect(member.getByText('Đã lưu phản hồi')).toBeVisible();
  }
  // Trang trưởng nhóm cập nhật realtime
  await expect(leader.locator('.progress-text')).toContainText('3 vé dự kiến');

  // Sở thích chỗ ngồi (GRP22): Bob muốn ngồi cạnh Alice
  await bob.locator('nz-select#nextTo').click();
  await bob.locator('nz-option-item', { hasText: aliceUser.fullName }).click();
  await bob.getByRole('button', { name: 'Lưu sở thích' }).click();
  await expect(bob.getByText('Đã lưu sở thích chỗ ngồi')).toBeVisible();
  await expect(leader.locator('.member', { hasText: bobUser.fullName })).toContainText(`cạnh ${aliceUser.fullName}`);

  // Đề xuất suất và chốt
  await leader.getByRole('button', { name: 'Đề xuất suất' }).click();
  await leader.locator('app-day-tabs button', { hasText: pick.dayMonth }).click();
  await leader.locator('.showtime-list .showtime:not([disabled])').first().click();
  await expect(leader.locator('.option')).toHaveCount(1);
  await leader.getByRole('button', { name: 'Chốt suất này' }).click();
  await leader.locator('.ant-modal-footer').getByRole('button', { name: 'Chốt' }).click();
  await expect(leader.locator('.title-row nz-tag')).toHaveText('Đã chốt suất');

  // Chọn ghế: hệ thống tự đề xuất, giữ phương án #1 rồi thanh toán
  await leader.getByRole('link', { name: 'Chọn ghế cho nhóm' }).click();
  await expect(leader.locator('.option').first()).toContainText('#1');
  await expect(leader.locator('.bar')).toContainText('3/3');
  await leader.getByRole('button', { name: 'Giữ ghế & thanh toán' }).click();
  await expect(leader).toHaveURL(/\/checkout\/MB/);
  await leader.getByRole('button', { name: 'Thanh toán', exact: true }).click();
  await leader.getByRole('button', { name: 'Thanh toán thành công' }).click();
  await expect(leader.getByText('Đặt vé thành công!')).toBeVisible();

  await leader.goto(groupUrl);
  await expect(leader.locator('.title-row nz-tag')).toHaveText('Đã đặt vé');
  await expect(leader.locator('nz-alert.seating')).toContainText('đáp ứng 1/1 mong muốn');
  for (const page of [leader, alice, bob]) {
    await page.goto('/account/tickets');
    await expect(page.locator('app-ticket-card')).toHaveCount(1);
  }
});
