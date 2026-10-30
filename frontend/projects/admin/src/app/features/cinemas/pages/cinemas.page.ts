import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ACTIVE_STATUS_LABELS, Cinema, CinemaRequest } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CinemaFormModalComponent } from '../components/cinema-form-modal.component';
import { CinemasStore } from '../cinemas.store';

/** Danh sách rạp (CIN01, CIN02). */
@Component({
  selector: 'adm-cinemas-page',
  imports: [RouterLink, NzCardModule, NzTableModule, NzButtonModule, NzTagModule, CinemaFormModalComponent],
  template: `
    <nz-card nzTitle="Rạp chiếu" [nzExtra]="extra">
      <nz-table #table nzSize="middle" [nzData]="store.cinemas()" [nzLoading]="store.loading()" [nzScroll]="{ x: '800px' }">
        <thead>
          <tr>
            <th>Tên rạp</th>
            <th>Thành phố</th>
            <th>Địa chỉ</th>
            <th>Số phòng</th>
            <th>Trạng thái</th>
            <th nzWidth="180px">Thao tác</th>
          </tr>
        </thead>
        <tbody>
          @for (c of table.data; track c.id) {
            <tr>
              <td><a [routerLink]="[c.id]" class="name">{{ c.name }}</a></td>
              <td>{{ c.city }}</td>
              <td>{{ c.address }}</td>
              <td>{{ c.roomCount }}</td>
              <td>
                <nz-tag [nzColor]="c.status === 'ACTIVE' ? 'success' : 'default'">{{ statusLabels[c.status] }}</nz-tag>
              </td>
              <td class="actions">
                <a nz-button nzSize="small" [routerLink]="[c.id]">Phòng chiếu</a>
                <button nz-button nzSize="small" (click)="editing.set(c)">Sửa</button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>
    <ng-template #extra>
      <button nz-button nzType="primary" (click)="editing.set(null)">Thêm rạp</button>
    </ng-template>
    <adm-cinema-form-modal [cinema]="editing()" [saving]="store.saving()" (closed)="editing.set(undefined)" (saved)="save($event)" />
  `,
  styles: `
    .name {
      font-weight: 500;
    }

    .actions > * + * {
      margin-left: 8px;
    }
  `,
})
export class CinemasPage implements OnInit {
  protected readonly store = inject(CinemasStore);
  private readonly message = inject(NzMessageService);

  protected readonly statusLabels = ACTIVE_STATUS_LABELS;
  protected readonly editing = signal<Cinema | null | undefined>(undefined);

  ngOnInit(): void {
    this.store.loadCinemas();
  }

  protected save(request: CinemaRequest): void {
    this.store.saveCinema(this.editing()?.id ?? null, request).subscribe(() => {
      this.message.success('Đã lưu rạp');
      this.editing.set(undefined);
    });
  }
}
