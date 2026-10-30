import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminRefund, RefundStatus, VN_OFFSET, VndPipe } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSegmentedModule } from 'ng-zorro-antd/segmented';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { RefundsStore } from './refunds.store';

const REASONS: Partial<Record<string, string>> = {
  CUSTOMER_CANCEL: 'Khách hủy',
  CANCELLED_BY_USER: 'Khách hủy',
  GROUP_CANCEL: 'Trưởng nhóm hủy',
  SHOWTIME_CANCELLED: 'Rạp hủy suất',
  SEATS_UNAVAILABLE: 'Thanh toán trễ, ghế đã bán',
};

const STATUS: Record<RefundStatus, { label: string; color: string }> = {
  PENDING: { label: 'Chờ hoàn', color: 'orange' },
  COMPLETED: { label: 'Đã hoàn', color: 'green' },
  FAILED: { label: 'Lỗi', color: 'red' },
};

/** Hoàn tiền thủ công (ORDADM-R, BR10, BR17). */
@Component({
  selector: 'adm-refunds-page',
  providers: [RefundsStore],
  imports: [
    DatePipe,
    FormsModule,
    VndPipe,
    NzButtonModule,
    NzCardModule,
    NzInputModule,
    NzModalModule,
    NzSegmentedModule,
    NzTableModule,
    NzTagModule,
  ],
  template: `
    <nz-card nzTitle="Hoàn tiền" [nzExtra]="filter">
      <p class="muted">
        Cổng giả lập hoàn tự động. Với VNPay, hãy hoàn trên trang quản trị merchant của VNPay rồi bấm "Xác nhận đã
        hoàn" để đơn chuyển sang "Đã hoàn tiền".
      </p>
      <nz-table
        #table
        [nzData]="store.page()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.page()?.totalElements ?? 0"
        [nzPageIndex]="(store.page()?.page ?? 0) + 1"
        [nzPageSize]="20"
        [nzLoading]="store.loading()"
        (nzPageIndexChange)="store.load($event - 1)"
        [nzScroll]="{ x: '900px' }"
      >
        <thead>
          <tr>
            <th>Đơn</th>
            <th>Lý do</th>
            <th>Cổng</th>
            <th nzAlign="right">Tổng đơn</th>
            <th nzAlign="right">Số tiền hoàn</th>
            <th>Trạng thái</th>
            <th>Tạo lúc</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (r of table.data; track r.id) {
            <tr>
              <td><code>{{ r.orderCode }}</code></td>
              <td>{{ reasons[r.reason] ?? r.reason }}</td>
              <td>{{ r.provider ?? '—' }}</td>
              <td nzAlign="right">{{ r.orderTotal | vnd }}</td>
              <td nzAlign="right"><strong>{{ r.amount | vnd }}</strong></td>
              <td>
                <nz-tag [nzColor]="statuses[r.status].color">{{ statuses[r.status].label }}</nz-tag>
                @if (r.note) {
                  <div class="muted small">{{ r.note }}</div>
                }
              </td>
              <td>{{ r.createdAt | date: 'HH:mm dd/MM/yyyy' : vn }}</td>
              <td>
                @if (r.status === 'PENDING') {
                  <button nz-button nzType="primary" nzSize="small" (click)="open(r)">Xác nhận đã hoàn</button>
                }
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>
    <ng-template #filter>
      <nz-segmented [nzOptions]="filterOptions" [ngModel]="filterValue()" (ngModelChange)="changeFilter($event)" />
    </ng-template>

    <nz-modal
      [nzVisible]="selected() !== null"
      nzTitle="Xác nhận đã hoàn tiền"
      nzOkText="Xác nhận"
      [nzOkLoading]="saving()"
      (nzOnOk)="confirm()"
      (nzOnCancel)="selected.set(null)"
    >
      <ng-container *nzModalContent>
        @if (selected(); as r) {
          <p>Đơn <code>{{ r.orderCode }}</code> — hoàn <strong>{{ r.amount | vnd }}</strong> qua {{ r.provider }}.</p>
          <label for="note">Ghi chú (mã giao dịch hoàn trên trang merchant)</label>
          <input id="note" nz-input [(ngModel)]="note" maxlength="500" />
        }
      </ng-container>
    </nz-modal>
  `,
  styles: `
    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .small {
      font-size: 12px;
    }
  `,
})
export class RefundsPage implements OnInit {
  protected readonly store = inject(RefundsStore);
  private readonly message = inject(NzMessageService);

  protected readonly vn = VN_OFFSET;
  protected readonly reasons = REASONS;
  protected readonly statuses = STATUS;
  protected readonly filterOptions = [
    { label: 'Chờ hoàn', value: 'PENDING' },
    { label: 'Đã hoàn', value: 'COMPLETED' },
    { label: 'Tất cả', value: 'ALL' },
  ];
  protected readonly filterValue = signal('PENDING');
  protected readonly selected = signal<AdminRefund | null>(null);
  protected readonly saving = signal(false);
  protected note = '';

  ngOnInit(): void {
    this.store.load();
  }

  protected changeFilter(value: string): void {
    this.filterValue.set(value);
    this.store.setStatus(value === 'ALL' ? null : (value as RefundStatus));
  }

  protected open(refund: AdminRefund): void {
    this.note = '';
    this.selected.set(refund);
  }

  protected confirm(): void {
    const refund = this.selected();
    if (!refund) {
      return;
    }
    this.saving.set(true);
    this.store.complete(refund.id, this.note.trim() || null).subscribe({
      next: () => {
        this.saving.set(false);
        this.selected.set(null);
        this.message.success('Đã xác nhận hoàn tiền');
      },
      error: () => this.saving.set(false),
    });
  }
}
