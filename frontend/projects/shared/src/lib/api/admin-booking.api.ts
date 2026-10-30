import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { PageResponse } from '../models/api-response.model';
import { AdminRefund, CheckinResult, RefundStatus } from '../models/booking.model';

/** Soát vé (CHK01) và hoàn tiền thủ công (ORDADM-R) cho trang quản trị. */
@Injectable({ providedIn: 'root' })
export class AdminBookingApi {
  private readonly api = inject(ApiClient);

  /** Lỗi (vé đã soát, QR sai) do trang soát vé tự hiển thị lớn trên màn hình. */
  checkin(payload: string): Observable<CheckinResult> {
    return this.api.post<CheckinResult>('/admin/checkin', { payload }, { context: skipErrorMessage() });
  }

  refunds(status: RefundStatus | null, page: number, size: number): Observable<PageResponse<AdminRefund>> {
    return this.api.get<PageResponse<AdminRefund>>('/admin/refunds', { params: { status, page, size } });
  }

  completeRefund(id: number, note: string | null): Observable<unknown> {
    return this.api.post(`/admin/refunds/${id}/complete`, { note });
  }
}
