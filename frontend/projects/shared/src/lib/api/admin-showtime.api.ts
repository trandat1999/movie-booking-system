import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { PageResponse } from '../models/api-response.model';
import {
  BasePrice,
  DayType,
  Holiday,
  PriceQuote,
  ScreenFormat,
  Setting,
  Showtime,
  ShowtimeQuery,
  ShowtimeRequest,
  ShowtimeUpdateRequest,
} from '../models/catalog.model';

/** Quản trị lịch chiếu, giá vé, ngày lễ và cấu hình hệ thống. */
@Injectable({ providedIn: 'root' })
export class AdminShowtimeApi {
  private readonly api = inject(ApiClient);

  search(query: ShowtimeQuery): Observable<PageResponse<Showtime>> {
    return this.api.get<PageResponse<Showtime>>('/admin/showtimes', {
      params: {
        cinemaId: query.cinemaId,
        roomId: query.roomId,
        movieId: query.movieId,
        date: query.date,
        status: query.status,
        page: query.page,
        size: query.size,
      },
    });
  }

  /** Lỗi trùng lịch (kèm danh sách suất trùng) được form tự hiển thị. */
  create(request: ShowtimeRequest): Observable<Showtime> {
    return this.api.post<Showtime>('/admin/showtimes', request, { context: skipErrorMessage() });
  }

  update(id: number, request: ShowtimeUpdateRequest): Observable<Showtime> {
    return this.api.put<Showtime>(`/admin/showtimes/${id}`, request, { context: skipErrorMessage() });
  }

  cancel(id: number): Observable<Showtime> {
    return this.api.post<Showtime>(`/admin/showtimes/${id}/cancel`);
  }

  quote(roomId: number, movieId: number | null, format: ScreenFormat, startAt: string): Observable<PriceQuote> {
    return this.api.get<PriceQuote>('/admin/pricing/quote', { params: { roomId, movieId, format, startAt } });
  }

  listBasePrices(): Observable<BasePrice[]> {
    return this.api.get<BasePrice[]>('/admin/pricing/base-prices');
  }

  updateBasePrices(items: { format: ScreenFormat; dayType: DayType; price: number }[]): Observable<BasePrice[]> {
    return this.api.put<BasePrice[]>('/admin/pricing/base-prices', { items });
  }

  listHolidays(year: number): Observable<Holiday[]> {
    return this.api.get<Holiday[]>('/admin/pricing/holidays', { params: { year } });
  }

  addHoliday(holiday: Holiday): Observable<Holiday> {
    return this.api.post<Holiday>('/admin/pricing/holidays', holiday);
  }

  deleteHoliday(date: string): Observable<void> {
    return this.api.delete<void>(`/admin/pricing/holidays/${date}`);
  }

  listSettings(): Observable<Setting[]> {
    return this.api.get<Setting[]>('/admin/settings');
  }

  updateSetting(key: string, value: string): Observable<Setting> {
    return this.api.put<Setting>(`/admin/settings/${key}`, { value });
  }
}
