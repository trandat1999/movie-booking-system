import { Component, computed, DestroyRef, effect, inject, input, output, signal } from '@angular/core';

/**
 * Đồng hồ đếm ngược tới {@link expiresAt}. Đồng hồ máy người dùng có thể lệch, nên hiệu chỉnh theo
 * {@link serverTime} (giờ máy chủ lúc trả dữ liệu).
 */
@Component({
  selector: 'app-countdown',
  template: `<span class="countdown" [class.urgent]="remaining() <= 60">{{ text() }}</span>`,
  styles: `
    .countdown {
      font-variant-numeric: tabular-nums;
      font-weight: 600;
    }

    .urgent {
      color: #cf1322;
    }
  `,
})
export class CountdownComponent {
  readonly expiresAt = input.required<string>();
  readonly serverTime = input.required<string>();
  readonly expired = output<void>();

  private readonly now = signal(Date.now());
  /** serverTime − giờ máy người dùng lúc nhận dữ liệu */
  private readonly offset = computed(() => new Date(this.serverTime()).getTime() - Date.now());

  /** Số giây còn lại */
  readonly remaining = computed(() =>
    Math.max(0, Math.floor((new Date(this.expiresAt()).getTime() - (this.now() + this.offset())) / 1000)),
  );

  protected readonly text = computed(() => {
    const seconds = this.remaining();
    return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  });

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
    let fired = false;
    effect(() => {
      if (this.remaining() === 0 && !fired) {
        fired = true;
        this.expired.emit();
      }
    });
  }
}
