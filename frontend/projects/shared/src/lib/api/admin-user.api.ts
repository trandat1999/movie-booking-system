import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { PageResponse } from '../models/api-response.model';
import { AdminUser, AdminUserQuery, Role, UserStatus } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AdminUserApi {
  private readonly api = inject(ApiClient);

  search(query: AdminUserQuery): Observable<PageResponse<AdminUser>> {
    return this.api.get<PageResponse<AdminUser>>('/admin/users', {
      params: { q: query.q, status: query.status, role: query.role, page: query.page, size: query.size },
    });
  }

  get(id: number): Observable<AdminUser> {
    return this.api.get<AdminUser>(`/admin/users/${id}`);
  }

  updateStatus(id: number, status: UserStatus): Observable<AdminUser> {
    return this.api.patch<AdminUser>(`/admin/users/${id}/status`, { status });
  }

  updateRoles(id: number, roleCodes: string[]): Observable<AdminUser> {
    return this.api.put<AdminUser>(`/admin/users/${id}/roles`, { roleCodes });
  }

  listRoles(): Observable<Role[]> {
    return this.api.get<Role[]>('/admin/roles');
  }
}
