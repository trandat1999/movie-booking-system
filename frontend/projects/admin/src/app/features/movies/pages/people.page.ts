import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Person } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzTableModule } from 'ng-zorro-antd/table';

import { CatalogStore } from '../catalog.store';

/** Quản lý đạo diễn, diễn viên (MOVADM06, MOVADM07). */
@Component({
  selector: 'adm-people-page',
  imports: [
    FormsModule,
    RouterLink,
    NzCardModule,
    NzTableModule,
    NzButtonModule,
    NzInputModule,
    NzIconModule,
    NzFormModule,
    NzModalModule,
    NzPopconfirmModule,
  ],
  template: `
    <nz-card nzTitle="Đạo diễn & diễn viên" [nzExtra]="extra">
      <nz-input-group nzPrefixIcon="search" class="search">
        <input nz-input placeholder="Tìm theo tên" [(ngModel)]="keyword" (keyup.enter)="store.searchPeople(keyword.trim())" aria-label="Tìm theo tên" />
      </nz-input-group>
      <nz-table
        #table
        nzSize="middle"
        [nzData]="store.people()"
        [nzLoading]="store.loadingPeople()"
        [nzFrontPagination]="false"
        [nzTotal]="store.peopleTotal()"
        [nzPageIndex]="store.peopleQuery().page + 1"
        [nzPageSize]="store.peopleQuery().size"
        (nzPageIndexChange)="store.changePeoplePage($event)"
      >
        <thead>
          <tr>
            <th>Họ tên</th>
            <th>Ảnh</th>
            <th nzWidth="160px">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          @for (p of table.data; track p.id) {
            <tr>
              <td>{{ p.fullName }}</td>
              <td class="muted">{{ p.avatarUrl ?? '—' }}</td>
              <td class="actions">
                <button nz-button nzSize="small" (click)="open(p)">Sửa</button>
                <button nz-button nzSize="small" nzDanger nz-popconfirm nzPopconfirmTitle="Xóa người này?" (nzOnConfirm)="remove(p)">
                  Xóa
                </button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>

    <ng-template #extra>
      <a nz-button routerLink="/movies">← Phim</a>
      <button nz-button nzType="primary" (click)="open(null)" class="add">Thêm người</button>
    </ng-template>

    <nz-modal
      [nzVisible]="editing() !== undefined"
      [nzTitle]="editing() ? 'Sửa thông tin' : 'Thêm đạo diễn/diễn viên'"
      nzOkText="Lưu"
      nzCancelText="Hủy"
      [nzOkLoading]="store.saving()"
      [nzOkDisabled]="!fullName.trim()"
      (nzOnCancel)="editing.set(undefined)"
      (nzOnOk)="save()"
    >
      <ng-container *nzModalContent>
        <form nz-form nzLayout="vertical">
          <nz-form-item>
            <nz-form-label nzFor="fullName" nzRequired>Họ tên</nz-form-label>
            <nz-form-control>
              <input nz-input id="fullName" name="fullName" [(ngModel)]="fullName" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="avatarUrl">Ảnh (URL)</nz-form-label>
            <nz-form-control>
              <input nz-input id="avatarUrl" name="avatarUrl" [(ngModel)]="avatarUrl" placeholder="https://..." />
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>
  `,
  styles: `
    .search {
      max-width: 320px;
      margin-bottom: 16px;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .actions button + button,
    .add {
      margin-left: 8px;
    }
  `,
})
export class PeoplePage implements OnInit {
  protected readonly store = inject(CatalogStore);
  private readonly message = inject(NzMessageService);

  protected readonly editing = signal<Person | null | undefined>(undefined);
  protected keyword = '';
  protected fullName = '';
  protected avatarUrl = '';

  ngOnInit(): void {
    this.store.loadPeople();
  }

  protected open(person: Person | null): void {
    this.fullName = person?.fullName ?? '';
    this.avatarUrl = person?.avatarUrl ?? '';
    this.editing.set(person);
  }

  protected save(): void {
    if (!this.fullName.trim()) {
      return;
    }
    this.store
      .savePerson(this.editing()?.id ?? null, this.fullName.trim(), this.avatarUrl.trim() || null)
      .subscribe(() => {
        this.message.success('Đã lưu');
        this.editing.set(undefined);
      });
  }

  protected remove(person: Person): void {
    this.store.deletePerson(person.id).subscribe(() => this.message.success(`Đã xóa ${person.fullName}`));
  }
}
