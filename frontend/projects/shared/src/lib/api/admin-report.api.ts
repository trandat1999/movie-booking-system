import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { API_BASE_URL } from '../core/config/api-config';
import { PageResponse } from '../models/api-response.model';
import {
  AdminGroupDetail,
  AdminGroupRow,
  AdminOrderDetail,
  AdminOrderQuery,
  AdminOrderRow,
  AuditLog,
  GroupStats,
  OverviewStats,
  Promotion,
  PromotionRequest,
  StatsFilter,
} from '../models/admin-report.model';
import { GroupStatus } from '../models/group.model';

/** API quản trị M8: thống kê, đơn, nhóm, khuyến mãi, nhật ký. */
@Injectable({ providedIn: 'root' })
export class AdminReportApi {
  private readonly api = inject(ApiClient);
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  overview(filter: StatsFilter): Observable<OverviewStats> {
    return this.api.get<OverviewStats>('/admin/stats/overview', { params: { ...filter } });
  }

  groupStats(filter: StatsFilter): Observable<GroupStats> {
    return this.api.get<GroupStats>('/admin/stats/groups', {
      params: { from: filter.from, to: filter.to, movieId: filter.movieId },
    });
  }

  /** CSV đơn hàng (blob) — tải qua HttpClient để kèm access token. */
  exportOrdersCsv(filter: StatsFilter): Observable<Blob> {
    let params = new HttpParams();
    Object.entries(filter).forEach(([k, v]) => {
      if (v !== null && v !== undefined) {
        params = params.set(k, String(v));
      }
    });
    return this.http.get(`${this.baseUrl}/admin/reports/orders.csv`, { params, responseType: 'blob' });
  }

  orders(query: AdminOrderQuery): Observable<PageResponse<AdminOrderRow>> {
    return this.api.get<PageResponse<AdminOrderRow>>('/admin/orders', { params: { ...query } });
  }

  order(code: string): Observable<AdminOrderDetail> {
    return this.api.get<AdminOrderDetail>(`/admin/orders/${encodeURIComponent(code)}`);
  }

  cancelOrder(code: string): Observable<AdminOrderDetail> {
    return this.api.post<AdminOrderDetail>(`/admin/orders/${encodeURIComponent(code)}/cancel`);
  }

  groups(status: GroupStatus | null, q: string | null, page: number, size: number): Observable<PageResponse<AdminGroupRow>> {
    return this.api.get<PageResponse<AdminGroupRow>>('/admin/groups', { params: { status, q, page, size } });
  }

  group(id: number): Observable<AdminGroupDetail> {
    return this.api.get<AdminGroupDetail>(`/admin/groups/${id}`);
  }

  promotions(q: string | null, page: number, size: number): Observable<PageResponse<Promotion>> {
    return this.api.get<PageResponse<Promotion>>('/admin/promotions', { params: { q, page, size } });
  }

  createPromotion(request: PromotionRequest): Observable<Promotion> {
    return this.api.post<Promotion>('/admin/promotions', request);
  }

  updatePromotion(id: number, request: PromotionRequest): Observable<Promotion> {
    return this.api.put<Promotion>(`/admin/promotions/${id}`, request);
  }

  auditLogs(action: string | null, resource: string | null, page: number, size: number): Observable<PageResponse<AuditLog>> {
    return this.api.get<PageResponse<AuditLog>>('/admin/audit-logs', { params: { action, resource, page, size } });
  }
}
