import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { AdminBookingApi, CheckinResult, resolveErrorMessage } from '@mb/shared';
import { finalize } from 'rxjs';

export interface CheckinOutcome {
  ok: boolean;
  message: string;
  result: CheckinResult | null;
  at: Date;
}

/** Bỏ qua cùng một mã quét lặp lại trong khoảng này (camera đọc liên tục khi mã còn trong khung). */
const DUPLICATE_WINDOW_MS = 4000;

/** Soát vé (CHK01): gửi nội dung QR lên backend, giữ lịch sử quét gần nhất. */
@Injectable()
export class CheckinStore {
  private readonly api = inject(AdminBookingApi);

  readonly last = signal<CheckinOutcome | null>(null);
  readonly history = signal<CheckinOutcome[]>([]);
  readonly busy = signal(false);

  private lastPayload = '';
  private lastAt = 0;

  scan(payload: string): void {
    const value = payload.trim();
    const now = Date.now();
    if (!value || this.busy() || (value === this.lastPayload && now - this.lastAt < DUPLICATE_WINDOW_MS)) {
      return;
    }
    this.lastPayload = value;
    this.lastAt = now;
    this.busy.set(true);
    this.api
      .checkin(value)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (result) =>
          this.push({
            ok: true,
            message: `Hợp lệ — đã soát ${result.checkedIn} vé (đơn ${result.orderCode})`,
            result,
            at: new Date(),
          }),
        error: (e: HttpErrorResponse) =>
          this.push({ ok: false, message: resolveErrorMessage(e), result: null, at: new Date() }),
      });
  }

  private push(outcome: CheckinOutcome): void {
    this.last.set(outcome);
    this.history.update((list) => [outcome, ...list].slice(0, 20));
  }
}
