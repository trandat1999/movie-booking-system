import { inject, Injectable, signal } from '@angular/core';
import { AdminOrderDetail, AdminOrderQuery, AdminOrderRow, AdminReportApi, PageResponse } from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Quản lý đơn (ORDADM): tìm kiếm, chi tiết, lịch sử, hủy + hoàn tiền (ORDADM-R). */
@Injectable()
export class OrdersStore {
  private readonly api = inject(AdminReportApi);

  readonly page = signal<PageResponse<AdminOrderRow> | null>(null);
  readonly detail = signal<AdminOrderDetail | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);

  query: AdminOrderQuery = { q: null, email: null, status: null, from: null, to: null, page: 0, size: 20 };

  search(page = 0): void {
    this.query = { ...this.query, page };
    this.loading.set(true);
    this.api
      .orders(this.query)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => this.page.set(result));
  }

  open(code: string): void {
    this.detail.set(null);
    this.api.order(code).subscribe((detail) => this.detail.set(detail));
  }

  close(): void {
    this.detail.set(null);
  }

  cancel(code: string): Observable<AdminOrderDetail> {
    this.busy.set(true);
    return this.api.cancelOrder(code).pipe(
      tap((detail) => {
        this.detail.set(detail);
        this.search(this.query.page);
      }),
      finalize(() => this.busy.set(false)),
    );
  }
}
