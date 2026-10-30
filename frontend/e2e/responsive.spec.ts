import { expect, Page, test } from '@playwright/test';

import { ADMIN_URL, API, findShowtime, loginUi, registerUser } from './helpers';

/** Responsive (PLAN.md M9): ở bề rộng điện thoại (375 px) không trang nào bị cuộn ngang. */
test.use({ viewport: { width: 375, height: 812 }, isMobile: true, hasTouch: true });

async function expectNoHorizontalScroll(page: Page, path: string): Promise<void> {
  await page.goto(path);
  await page.waitForLoadState('networkidle');
  const overflow = await page.evaluate(() => {
    const doc = document.documentElement;
    const offenders = [...document.querySelectorAll('body *')]
      .filter((el) => el.getBoundingClientRect().right > doc.clientWidth + 1 && getComputedStyle(el).position !== 'fixed')
      .filter((el) => !el.closest('.seat-scroll, .ant-table-content, .table-scroll, [data-scroll-x]'))
      .slice(0, 5)
      .map((el) => `${el.tagName.toLowerCase()}.${[...el.classList].join('.')}`);
    return { scroll: doc.scrollWidth - doc.clientWidth, offenders };
  });
  const name = path.replace(/[^a-z0-9]+/gi, '-').replace(/^-|-$/g, '') || 'home';
  await page.screenshot({ path: `test-results/responsive/${name}.png`, fullPage: true });
  expect(overflow.scroll, `${path} tràn ngang: ${overflow.offenders.join(', ')}`).toBeLessThanOrEqual(1);
}

test('trang khách hàng không tràn ngang trên điện thoại', async ({ page, request }) => {
  const movie = (await (await request.get(`${API}/movies?status=NOW_SHOWING&size=1`)).json()).data.items[0];
  const pick = await findShowtime(request, 1);
  for (const path of ['/', '/movies', `/movies/${movie.slug}`, '/cinemas', `/showtimes/${pick.showtimeId}`, '/login', '/register']) {
    await expectNoHorizontalScroll(page, path);
  }
  const user = await registerUser(request, 'mobile');
  await loginUi(page, user.email);
  for (const path of ['/groups', `/groups/new?movieId=${pick.movieId}`, '/account/orders', '/account/tickets', '/account']) {
    await expectNoHorizontalScroll(page, path);
  }
  // Menu chính nằm trong ngăn kéo khi màn hình hẹp
  await page.goto('/');
  await page.getByRole('button', { name: 'Mở menu' }).click();
  await page.locator('.ant-drawer').getByRole('link', { name: 'Đặt vé nhóm' }).click();
  await expect(page).toHaveURL(/\/groups$/);
  await expect(page.locator('.ant-drawer-open')).toHaveCount(0);
});

test('trang soát vé (nhân viên dùng điện thoại) không tràn ngang', async ({ page }) => {
  await loginUi(page, 'staff@moviebooking.local', 'Password123', ADMIN_URL);
  await expectNoHorizontalScroll(page, `${ADMIN_URL}/checkin`);
});
