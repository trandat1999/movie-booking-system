import { DatePipe, JsonPipe } from '@angular/common';
import { Component, inject, Injectable, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminReportApi, AuditLog, PageResponse, VN_OFFSET } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTableModule } from 'ng-zorro-antd/table';
import { finalize } from 'rxjs';

@Injectable()
export class AuditStore {
  private readonly api = inject(AdminReportApi);

  readonly page = signal<PageResponse<AuditLog> | null>(null);
  readonly loading = signal(false);

  action = '';
  resource = '';

  load(page = 0): void {
    this.loading.set(true);
    this.api
      .auditLogs(this.action.trim() || null, this.resource.trim() || null, page, 20)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => this.page.set(result));
  }
}

/** Nhật ký thao tác (SYS02, BR12): ai làm gì, lúc nào, từ IP nào. */
@Component({
  selector: 'adm-audit-page',
  providers: [AuditStore],
  imports: [DatePipe, JsonPipe, FormsModule, NzButtonModule, NzCardModule, NzInputModule, NzTableModule],
  template: `
    <nz-card nzTitle="Nhật ký thao tác">
      <form class="filters" (ngSubmit)="store.load(0)">
        <input nz-input name="action" [(ngModel)]="store.action" placeholder="Hành động, ví dụ ORDER_ADMIN_CANCEL" aria-label="Hành động" class="w280" />
        <input nz-input name="resource" [(ngModel)]="store.resource" placeholder="Tài nguyên, ví dụ order" aria-label="Tài nguyên" class="w200" />
        <button nz-button nzType="primary" type="submit">Lọc</button>
      </form>
      <nz-table
        #table
        [nzData]="store.page()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.page()?.totalElements ?? 0"
        [nzPageIndex]="(store.page()?.page ?? 0) + 1"
        [nzPageSize]="20"
        [nzLoading]="store.loading()"
        (nzPageIndexChange)="store.load($event - 1)"
        [nzScroll]="{ x: '960px' }"
      >
        <thead>
          <tr>
            <th>Lúc</th>
            <th>Người thực hiện</th>
            <th>Hành động</th>
            <th>Tài nguyên</th>
            <th>Chi tiết</th>
            <th>IP</th>
          </tr>
        </thead>
        <tbody>
          @for (log of table.data; track log.id) {
            <tr>
              <td>{{ log.createdAt | date: 'HH:mm:ss dd/MM/yyyy' : vn }}</td>
              <td>{{ log.actorEmail ?? '—' }}</td>
              <td><code>{{ log.action }}</code></td>
              <td>{{ log.resource }}{{ log.resourceId ? ' #' + log.resourceId : '' }}</td>
              <td><code class="data">{{ log.data ? (log.data | json) : '' }}</code></td>
              <td>{{ log.ipAddress }}</td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>
  `,
  styles: `
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 16px;
    }

    .w200 {
      width: 200px;
    }

    .w280 {
      width: 280px;
    }

    .data {
      display: block;
      max-width: 360px;
      font-size: 11px;
      white-space: pre-wrap;
      word-break: break-all;
    }
  `,
})
export class AuditPage implements OnInit {
  protected readonly store = inject(AuditStore);
  protected readonly vn = VN_OFFSET;

  ngOnInit(): void {
    this.store.load();
  }
}
