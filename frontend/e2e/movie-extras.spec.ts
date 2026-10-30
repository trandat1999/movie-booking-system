import { expect, test } from '@playwright/test';

import { API, apiLogin, bookViaApi, findShowtime, loginUi, registerUser, signQr } from './helpers';

/** M10: phim yêu thích, phim tương tự, đánh giá phim sau khi vé đã được soát. */
test('khán giả đã xem phim yêu thích và đánh giá phim', async ({ page, request }) => {
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
  const slug = (await (await request.get(`${API}/showtimes/${pick.showtimeId}`)).json()).data.showtime.movieSlug;
  const viewer = await registerUser(request, 'viewer');
  const { ticketCodes } = await bookViaApi(request, await apiLogin(request, viewer.email), pick);
  const staffToken = await apiLogin(request, 'staff@moviebooking.local', 'Password123');
  const checkin = await request.post(`${API}/admin/checkin`, {
    headers: { Authorization: `Bearer ${staffToken}` },
    data: { payload: signQr(ticketCodes[0]) },
  });
  expect(checkin.ok(), await checkin.text()).toBeTruthy();

  await loginUi(page, viewer.email);
  await page.goto(`/movies/${slug}`);

  // Yêu thích → có trong trang Phim yêu thích
  await page.getByRole('button', { name: 'Yêu thích' }).click();
  await expect(page.getByRole('button', { name: 'Đã yêu thích' })).toBeVisible();

  // Đánh giá: đã xem nên có form
  const reviews = page.locator('app-movie-reviews');
  await expect(reviews).toContainText('Bạn đã xem phim này');
  await reviews.locator('form nz-rate li').nth(3).click();
  await reviews.locator('textarea').fill('Hình ảnh đẹp, nhạc hay');
  await reviews.getByRole('button', { name: 'Gửi đánh giá' }).click();
  await expect(reviews.locator('.review', { hasText: 'Hình ảnh đẹp, nhạc hay' })).toBeVisible();
  await expect(reviews.getByRole('button', { name: 'Cập nhật' })).toBeVisible();

  // Phim tương tự (dữ liệu mẫu có nhiều phim chung thể loại)
  await expect(page.getByRole('heading', { name: 'Phim tương tự' })).toBeVisible();

  await page.goto('/account/favorites');
  await expect(page.locator('app-movie-card')).toHaveCount(1);
});
