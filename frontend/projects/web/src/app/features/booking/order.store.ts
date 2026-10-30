import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  BookingApi,
  Order,
  OrderStatusMessage,
  PaymentCallbackResult,
  PaymentProviderInfo,
  RealtimeService,
} from '@mb/shared';
import { filter, finalize, interval, map, Observable, Subscription, switchMap, takeWhile, tap } from 'rxjs';

const POLL_INTERVAL_MS = 3000;

/** Một đơn hàng: thanh toán, hủy, theo dõi trạng thái sau thanh toán (BKG05–BKG08). */
@Injectable()
export class OrderStore {
  private readonly api = inject(BookingApi);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly order = signal<Order | null>(null);
  readonly notFound = signal(false);
  readonly busy = signal(false);

  readonly status = computed(() => this.order()?.status ?? null);

  private watchSub?: Subscription;

  load(code: string): void {
    this.order.set(null);
    this.notFound.set(false);
    this.reload(code);
  }

  reload(code: string): void {
    this.api.getOrder(code).subscribe({
      next: (order) => this.order.set(order),
      error: () => this.notFound.set(true),
    });
  }

  /** @return URL của cổng thanh toán */
  pay(provider: string): Observable<string> {
    this.busy.set(true);
    return this.api.pay(this.order()!.code, provider).pipe(
      map((session) => session.redirectUrl),
      finalize(() => this.busy.set(false)),
    );
  }

  cancel(): Observable<Order> {
    this.busy.set(true);
    return this.api.cancelOrder(this.order()!.code).pipe(
      tap((order) => this.order.set(order)),
      finalize(() => this.busy.set(false)),
    );
  }

  readonly providers = signal<PaymentProviderInfo[]>([]);

  loadProviders(): void {
    this.api.providers().subscribe((list) => this.providers.set(list));
  }

  /** Trang return của VNPay: gửi kết quả đã ký về backend. */
  sendVnpayReturn(params: Record<string, string>): Observable<PaymentCallbackResult> {
    this.busy.set(true);
    return this.api.sendVnpayReturn(params).pipe(finalize(() => this.busy.set(false)));
  }

  /** Cổng giả lập gửi callback về backend. */
  sendMockCallback(params: Record<string, string>): Observable<PaymentCallbackResult> {
    this.busy.set(true);
    return this.api.sendMockIpn(params).pipe(finalize(() => this.busy.set(false)));
  }

  /**
   * Trong lúc đơn chờ xác nhận thanh toán: nghe `/user/queue/orders` và hỏi lại định kỳ (phòng khi mất kết nối
   * realtime) cho tới khi đơn rời trạng thái PENDING_PAYMENT.
   */
  watchUntilSettled(code: string): void {
    this.watchSub?.unsubscribe();
    this.watchSub = new Subscription();
    this.watchSub.add(
      this.realtime
        .watch<OrderStatusMessage>('/user/queue/orders')
        .pipe(
          filter((message) => message.orderCode === code),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe(() => this.reload(code)),
    );
    this.watchSub.add(
      interval(POLL_INTERVAL_MS)
        .pipe(
          takeWhile(() => this.status() === null || this.status() === 'PENDING_PAYMENT'),
          switchMap(() => this.api.getOrder(code)),
          takeUntilDestroyed(this.destroyRef),
        )
        .subscribe((order) => this.order.set(order)),
    );
  }
}
