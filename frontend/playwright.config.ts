import { defineConfig } from '@playwright/test';

/**
 * E2E (PLAN.md M9): chạy trên hệ thống dev đang chạy sẵn —
 *   backend  : cd backend && ./mvnw -pl api spring-boot:test-run   (cổng 8080, dữ liệu mẫu)
 *   web      : npx ng serve web    (cổng 4200)
 *   admin    : npx ng serve admin  (cổng 4300, cho test soát vé)
 * Dùng Chrome đã cài trên máy (channel "chrome"), không cần tải trình duyệt của Playwright.
 *
 *   npx playwright test
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'e2e-report' }]],
  use: {
    baseURL: process.env['WEB_URL'] ?? 'http://localhost:4200',
    channel: 'chrome',
    headless: true,
    locale: 'vi-VN',
    timezoneId: 'Asia/Ho_Chi_Minh',
    viewport: { width: 1280, height: 900 },
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
});
