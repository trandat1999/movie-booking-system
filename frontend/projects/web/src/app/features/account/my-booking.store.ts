import { inject, Injectable, signal } from '@angular/core';
import { BookingApi, MyTicket, OrderSummary, PageResponse } from '@mb/shared';
import { finalize } from 'rxjs';

/** Vé của tôi và lịch sử đơn hàng (BKG09, BKG10). */
@Injectable()
export class MyBookingStore {
  private readonly api = inject(BookingApi);

  readonly tickets = signal<MyTicket[] | null>(null);
  readonly orders = signal<PageResponse<OrderSummary> | null>(null);
  readonly loading = signal(false);

  loadTickets(past: boolean): void {
    this.tickets.set(null);
    this.api.myTickets(past).subscribe((tickets) => this.tickets.set(tickets));
  }

  /** @param page bắt đầu từ 0 */
  loadOrders(page: number, size: number): void {
    this.loading.set(true);
    this.api
      .myOrders(page, size)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((orders) => this.orders.set(orders));
  }
}
