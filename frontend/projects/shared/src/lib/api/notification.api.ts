import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { PageResponse } from '../models/api-response.model';
import { AppNotification } from '../models/booking.model';

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly api = inject(ApiClient);

  list(page: number, size: number): Observable<PageResponse<AppNotification>> {
    return this.api.get<PageResponse<AppNotification>>('/me/notifications', { params: { page, size } });
  }

  unreadCount(): Observable<number> {
    return this.api.get<{ count: number }>('/me/notifications/unread-count').pipe(map((r) => r.count));
  }

  markRead(id: number): Observable<void> {
    return this.api.post<void>(`/me/notifications/${id}/read`);
  }

  markAllRead(): Observable<void> {
    return this.api.post<void>('/me/notifications/read-all');
  }
}
