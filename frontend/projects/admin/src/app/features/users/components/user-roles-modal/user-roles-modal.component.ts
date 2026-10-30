import { Component, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminUser, Role } from '@mb/shared';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzModalModule } from 'ng-zorro-antd/modal';

@Component({
  selector: 'adm-user-roles-modal',
  imports: [FormsModule, NzModalModule, NzCheckboxModule],
  template: `
    <nz-modal
      [nzVisible]="!!user()"
      [nzTitle]="'Vai trò của ' + (user()?.fullName ?? '')"
      nzOkText="Lưu"
      nzCancelText="Hủy"
      [nzOkLoading]="saving()"
      [nzOkDisabled]="selected().size === 0"
      (nzOnCancel)="closed.emit()"
      (nzOnOk)="save()"
    >
      <ng-container *nzModalContent>
        @for (role of roles(); track role.code) {
          <div class="role">
            <!-- nz-checkbox tự render <input type="checkbox"> bên trong label -->
            <!-- eslint-disable-next-line @angular-eslint/template/label-has-associated-control -->
            <label nz-checkbox [ngModel]="selected().has(role.code)" (ngModelChange)="toggle(role.code, $event)">
              <strong>{{ role.name }}</strong> ({{ role.code }})
            </label>
            @if (role.description) {
              <div class="description">{{ role.description }}</div>
            }
          </div>
        }
        @if (selected().size === 0) {
          <div class="hint">Người dùng phải có ít nhất một vai trò.</div>
        }
      </ng-container>
    </nz-modal>
  `,
  styles: `
    .role {
      margin-bottom: 12px;
    }

    .description {
      margin-left: 24px;
      color: rgba(0, 0, 0, 0.45);
    }

    .hint {
      color: #ff4d4f;
    }
  `,
})
export class UserRolesModalComponent {
  /** Người dùng đang sửa; null = đóng modal */
  readonly user = input<AdminUser | null>(null);
  readonly roles = input<Role[]>([]);
  readonly saving = input(false);
  readonly closed = output<void>();
  readonly saved = output<string[]>();

  protected readonly selected = signal(new Set<string>());

  constructor() {
    effect(() => this.selected.set(new Set(this.user()?.roles.map((role) => role.code) ?? [])));
  }

  protected save(): void {
    this.saved.emit([...this.selected()]);
  }

  protected toggle(code: string, checked: boolean): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (checked) {
        next.add(code);
      } else {
        next.delete(code);
      }
      return next;
    });
  }
}
