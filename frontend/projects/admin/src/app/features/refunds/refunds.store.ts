import { inject, Injectable, signal } from '@angular/core';
import { AdminBookingApi, AdminRefund, PageResponse, RefundStatus } from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Yêu cầu hoàn tiền (ORDADM-R): PENDING là đơn cần hoàn thủ công trên trang merchant của cổng. */
@Injectable()
export class RefundsStore {
  private readonly api = inject(AdminBookingApi);

  readonly page = signal<PageResponse<AdminRefund> | null>(null);
  readonly status = signal<RefundStatus | null>('PENDING');
  readonly loading = signal(false);

  load(page = 0): void {
    this.loading.set(true);
    this.api
      .refunds(this.status(), page, 20)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => this.page.set(result));
  }

  setStatus(status: RefundStatus | null): void {
    this.status.set(status);
    this.load(0);
  }

  complete(id: number, note: string | null): Observable<unknown> {
    return this.api.completeRefund(id, note).pipe(tap(() => this.load(this.page()?.page ?? 0)));
  }
}
