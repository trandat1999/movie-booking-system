import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminUser, AuthStore, Permissions, UserStatus, UserStatusTagComponent } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { UserRolesModalComponent } from '../components/user-roles-modal/user-roles-modal.component';
import { UsersStore } from '../users.store';

@Component({
  selector: 'adm-users-page',
  imports: [
    DatePipe,
    FormsModule,
    NzCardModule,
    NzTableModule,
    NzInputModule,
    NzSelectModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzPopconfirmModule,
    UserStatusTagComponent,
    UserRolesModalComponent,
  ],
  templateUrl: './users.page.html',
  styleUrl: './users.page.less',
})
export class UsersPage implements OnInit {
  protected readonly store = inject(UsersStore);
  private readonly auth = inject(AuthStore);
  private readonly message = inject(NzMessageService);

  protected readonly canWrite = computed(() => this.auth.permissions().has(Permissions.USER_WRITE));
  protected readonly currentUserId = computed(() => this.auth.user()?.id);

  protected keyword = '';
  protected status: UserStatus | null = null;
  protected role: string | null = null;

  protected readonly editingRolesOf = signal<AdminUser | null>(null);

  ngOnInit(): void {
    this.store.load();
    this.store.loadRoles();
  }

  protected search(): void {
    this.store.search({ q: this.keyword.trim() || null, status: this.status, role: this.role });
  }

  protected toggleLock(user: AdminUser): void {
    const next: UserStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    this.store.updateStatus(user, next).subscribe(() =>
      this.message.success(next === 'LOCKED' ? `Đã khóa ${user.email}` : `Đã mở khóa ${user.email}`),
    );
  }

  protected saveRoles(roleCodes: string[]): void {
    const user = this.editingRolesOf();
    if (!user) {
      return;
    }
    this.store.updateRoles(user, roleCodes).subscribe(() => {
      this.message.success('Đã cập nhật vai trò. Quyền mới có hiệu lực khi người dùng làm mới phiên.');
      this.editingRolesOf.set(null);
    });
  }
}
