import { computed, inject, Injectable, signal } from '@angular/core';
import { AdminUser, AdminUserApi, AdminUserQuery, Role, UserStatus } from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Trạng thái trang quản lý người dùng (USRADM). */
@Injectable()
export class UsersStore {
  private readonly api = inject(AdminUserApi);

  readonly query = signal<AdminUserQuery>({ q: null, status: null, role: null, page: 0, size: 20 });
  readonly users = signal<AdminUser[]>([]);
  readonly total = signal(0);
  readonly loading = signal(false);
  readonly roles = signal<Role[]>([]);
  /** id người dùng đang được cập nhật (để hiện loading trên đúng dòng) */
  readonly updatingId = signal<number | null>(null);

  /** nz-pagination dùng trang bắt đầu từ 1 */
  readonly pageIndex = computed(() => this.query().page + 1);

  load(): void {
    this.loading.set(true);
    this.api
      .search(this.query())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((page) => {
        this.users.set(page.items);
        this.total.set(page.totalElements);
      });
  }

  loadRoles(): void {
    this.api.listRoles().subscribe((roles) => this.roles.set(roles));
  }

  search(filters: Pick<AdminUserQuery, 'q' | 'status' | 'role'>): void {
    this.query.update((query) => ({ ...query, ...filters, page: 0 }));
    this.load();
  }

  changePage(pageIndex: number, size: number): void {
    this.query.update((query) => ({ ...query, page: size === query.size ? pageIndex - 1 : 0, size }));
    this.load();
  }

  updateStatus(user: AdminUser, status: UserStatus): Observable<AdminUser> {
    return this.track(user.id, this.api.updateStatus(user.id, status));
  }

  updateRoles(user: AdminUser, roleCodes: string[]): Observable<AdminUser> {
    return this.track(user.id, this.api.updateRoles(user.id, roleCodes));
  }

  private track(userId: number, request: Observable<AdminUser>): Observable<AdminUser> {
    this.updatingId.set(userId);
    return request.pipe(
      tap((updated) => this.users.update((users) => users.map((u) => (u.id === updated.id ? updated : u)))),
      finalize(() => this.updatingId.set(null)),
    );
  }
}
