import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Genre } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzTableModule } from 'ng-zorro-antd/table';

import { CatalogStore } from '../catalog.store';

/** Quản lý thể loại phim (MOVADM05). */
@Component({
  selector: 'adm-genres-page',
  imports: [FormsModule, RouterLink, NzCardModule, NzTableModule, NzButtonModule, NzInputModule, NzModalModule, NzPopconfirmModule],
  template: `
    <nz-card nzTitle="Thể loại phim" [nzExtra]="extra">
      <nz-table #table nzSize="middle" [nzData]="store.genres()" [nzPageSize]="50" [nzHideOnSinglePage]="true">
        <thead>
          <tr>
            <th>Tên</th>
            <th>Slug</th>
            <th nzWidth="160px">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          @for (g of table.data; track g.id) {
            <tr>
              <td>{{ g.name }}</td>
              <td class="muted">{{ g.slug }}</td>
              <td class="actions">
                <button nz-button nzSize="small" (click)="open(g)">Sửa</button>
                <button nz-button nzSize="small" nzDanger nz-popconfirm nzPopconfirmTitle="Xóa thể loại này?" (nzOnConfirm)="remove(g)">
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
      <button nz-button nzType="primary" (click)="open(null)" class="add">Thêm thể loại</button>
    </ng-template>

    <nz-modal
      [nzVisible]="editing() !== undefined"
      [nzTitle]="editing() ? 'Sửa thể loại' : 'Thêm thể loại'"
      nzOkText="Lưu"
      nzCancelText="Hủy"
      [nzOkLoading]="store.saving()"
      [nzOkDisabled]="!name.trim()"
      (nzOnCancel)="editing.set(undefined)"
      (nzOnOk)="save()"
    >
      <ng-container *nzModalContent>
        <input nz-input [(ngModel)]="name" placeholder="Tên thể loại" aria-label="Tên thể loại" (keyup.enter)="save()" />
      </ng-container>
    </nz-modal>
  `,
  styles: `
    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .actions button + button,
    .add {
      margin-left: 8px;
    }
  `,
})
export class GenresPage implements OnInit {
  protected readonly store = inject(CatalogStore);
  private readonly message = inject(NzMessageService);

  /** undefined = đóng modal, null = thêm mới */
  protected readonly editing = signal<Genre | null | undefined>(undefined);
  protected name = '';

  ngOnInit(): void {
    this.store.loadGenres();
  }

  protected open(genre: Genre | null): void {
    this.name = genre?.name ?? '';
    this.editing.set(genre);
  }

  protected save(): void {
    if (!this.name.trim()) {
      return;
    }
    this.store.saveGenre(this.editing()?.id ?? null, this.name.trim()).subscribe(() => {
      this.message.success('Đã lưu thể loại');
      this.editing.set(undefined);
    });
  }

  protected remove(genre: Genre): void {
    this.store.deleteGenre(genre.id).subscribe(() => this.message.success(`Đã xóa ${genre.name}`));
  }
}
