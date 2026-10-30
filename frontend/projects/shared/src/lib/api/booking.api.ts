import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { PageResponse } from '../models/api-response.model';
import {
  HoldResponse,
  MyTicket,
  Order,
  OrderSummary,
  PaymentCallbackResult,
  PaymentProviderInfo,
  PaymentSession,
} from '../models/booking.model';

/** Giữ ghế, đơn hàng, thanh toán (M4). */
@Injectable({ providedIn: 'root' })
export class BookingApi {
  private readonly api = inject(ApiClient);

  /** Lỗi (ghế vừa bị giữ, ghế lẻ...) do trang tự hiển thị nên không hiện thông báo chung. */
  hold(showtimeId: number, seatIds: number[]): Observable<HoldResponse> {
    return this.api.post<HoldResponse>('/holds', { showtimeId, seatIds }, { context: skipErrorMessage() });
  }

  releaseHold(holdId: number): Observable<void> {
    return this.api.delete<void>(`/holds/${holdId}`);
  }

  createOrder(holdId: number): Observable<Order> {
    return this.api.post<Order>('/orders', { holdId });
  }

  getOrder(code: string): Observable<Order> {
    return this.api.get<Order>(`/orders/${encodeURIComponent(code)}`, { context: skipErrorMessage() });
  }

  cancelOrder(code: string): Observable<Order> {
    return this.api.post<Order>(`/orders/${encodeURIComponent(code)}/cancel`);
  }

  pay(code: string, provider: string): Observable<PaymentSession> {
    return this.api.post<PaymentSession>(`/orders/${encodeURIComponent(code)}/pay`, { provider });
  }

  /** Trang cổng giả lập gửi IPN về backend (không cần đăng nhập, xác thực bằng chữ ký). */
  sendMockIpn(params: Record<string, string>): Observable<PaymentCallbackResult> {
    return this.api.post<PaymentCallbackResult>('/payments/MOCK/ipn', params);
  }

  providers(): Observable<PaymentProviderInfo[]> {
    return this.api.get<PaymentProviderInfo[]>('/payments/providers');
  }

  /** Lỗi (mã sai, chưa đủ điều kiện) do trang thanh toán tự hiển thị cạnh ô nhập mã. */
  applyPromotion(code: string, promotionCode: string): Observable<Order> {
    return this.api.post<Order>(`/orders/${encodeURIComponent(code)}/promotion`, { code: promotionCode }, { context: skipErrorMessage() });
  }

  removePromotion(code: string): Observable<Order> {
    return this.api.delete<Order>(`/orders/${encodeURIComponent(code)}/promotion`);
  }

  /** Trang return của VNPay chuyển kết quả (đã ký) về backend; backend kiểm tra chữ ký và xử lý idempotent. */
  sendVnpayReturn(params: Record<string, string>): Observable<PaymentCallbackResult> {
    return this.api.post<PaymentCallbackResult>('/payments/VNPAY/ipn', params, { context: skipErrorMessage() });
  }

  myOrders(page: number, size: number): Observable<PageResponse<OrderSummary>> {
    return this.api.get<PageResponse<OrderSummary>>('/me/orders', { params: { page, size } });
  }

  myTickets(past: boolean): Observable<MyTicket[]> {
    return this.api.get<MyTicket[]>('/me/tickets', { params: { past } });
  }
}
