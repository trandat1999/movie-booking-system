import { DatePipe, JsonPipe } from '@angular/common';
import { Component, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AuthStore,
  ORDER_STATUS_COLORS,
  ORDER_STATUS_LABELS,
  OrderStatus,
  Permissions,
  TICKET_STATUS_LABELS,
  toIsoDate,
  toOptions,
  VN_OFFSET,
  VndPipe,
} from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTimelineModule } from 'ng-zorro-antd/timeline';

import { OrdersStore } from './orders.store';

/** Quản lý đơn đặt vé (ORDADM, ORDADM-R). */
@Component({
  selector: 'adm-orders-page',
  providers: [OrdersStore],
  imports: [
    DatePipe,
    JsonPipe,
    FormsModule,
    VndPipe,
    NzButtonModule,
    NzCardModule,
    NzDatePickerModule,
    NzDescriptionsModule,
    NzDrawerModule,
    NzInputModule,
    NzPopconfirmModule,
    NzSelectModule,
    NzTableModule,
    NzTagModule,
    NzTimelineModule,
  ],
  template: `
    <nz-card nzTitle="Đơn đặt vé">
      <form class="filters" (ngSubmit)="search()">
        <input nz-input name="q" [(ngModel)]="q" placeholder="Mã đơn" aria-label="Mã đơn" class="w160" />
        <input nz-input name="email" [(ngModel)]="email" placeholder="Email khách" aria-label="Email khách" class="w220" />
        <nz-select name="status" [(ngModel)]="status" nzAllowClear nzPlaceHolder="Mọi trạng thái" class="w180" aria-label="Trạng thái">
          @for (s of statusOptions; track s.value) {
            <nz-option [nzValue]="s.value" [nzLabel]="s.label" />
          }
        </nz-select>
        <nz-range-picker name="range" [(ngModel)]="range" nzFormat="dd/MM/yyyy" aria-label="Ngày tạo đơn" />
        <button nz-button nzType="primary" type="submit">Tìm</button>
      </form>
      <nz-table
        #table
        [nzData]="store.page()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.page()?.totalElements ?? 0"
        [nzPageIndex]="(store.page()?.page ?? 0) + 1"
        [nzPageSize]="20"
        [nzLoading]="store.loading()"
        (nzPageIndexChange)="store.search($event - 1)"
        [nzScroll]="{ x: '1000px' }"
      >
        <thead>
          <tr>
            <th>Mã đơn</th>
            <th>Khách hàng</th>
            <th>Phim / suất</th>
            <th>Ghế</th>
            <th nzAlign="right">Tổng tiền</th>
            <th>Trạng thái</th>
            <th>Tạo lúc</th>
          </tr>
        </thead>
        <tbody>
          @for (o of table.data; track o.code) {
            <tr class="row" tabindex="0" (click)="store.open(o.code)" (keyup.enter)="store.open(o.code)">
              <td>
                <code>{{ o.code }}</code>
                @if (o.groupId) {
                  <nz-tag nzColor="purple">Nhóm</nz-tag>
                }
              </td>
              <td>
                <div>{{ o.customer?.fullName }}</div>
                <div class="muted">{{ o.customer?.email }}</div>
              </td>
              <td>
                <div>{{ o.showtime.movieTitle }}</div>
                <div class="muted">{{ o.showtime.startAt | date: 'HH:mm dd/MM' : vn }} · {{ o.showtime.cinemaName }}</div>
              </td>
              <td>{{ o.seatCount }}</td>
              <td nzAlign="right">
                {{ o.total | vnd }}
                @if (o.promotionCode) {
                  <div class="muted">{{ o.promotionCode }} −{{ o.discount | vnd }}</div>
                }
              </td>
              <td><nz-tag [nzColor]="statusColors[o.status]">{{ statusLabels[o.status] }}</nz-tag></td>
              <td>{{ o.createdAt | date: 'HH:mm dd/MM/yyyy' : vn }}</td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>

    <nz-drawer [nzVisible]="store.detail() !== null" [nzWidth]="560" nzTitle="Chi tiết đơn" (nzOnClose)="store.close()">
      <ng-container *nzDrawerContent>
        @if (store.detail(); as d) {
          <nz-descriptions [nzColumn]="1" nzSize="small" nzBordered>
            <nz-descriptions-item nzTitle="Mã đơn"><code>{{ d.order.code }}</code></nz-descriptions-item>
            <nz-descriptions-item nzTitle="Trạng thái">
              <nz-tag [nzColor]="statusColors[d.order.status]">{{ statusLabels[d.order.status] }}</nz-tag>
            </nz-descriptions-item>
            <nz-descriptions-item nzTitle="Khách hàng">
              {{ d.customer?.fullName }} · {{ d.customer?.email }} {{ d.customer?.phone ? '· ' + d.customer?.phone : '' }}
            </nz-descriptions-item>
            <nz-descriptions-item nzTitle="Suất chiếu">
              {{ d.order.showtime.movieTitle }} — {{ d.order.showtime.startAt | date: 'HH:mm dd/MM/yyyy' : vn }},
              {{ d.order.showtime.cinemaName }} · {{ d.order.showtime.roomName }}
            </nz-descriptions-item>
            <nz-descriptions-item nzTitle="Ghế / vé">
              @for (t of d.order.tickets; track t.code) {
                <div>{{ t.seatLabel }} — <code>{{ t.code }}</code> ({{ ticketLabels[t.status] }})</div>
              } @empty {
                {{ seatLabels(d.order.items) }}
              }
            </nz-descriptions-item>
            <nz-descriptions-item nzTitle="Tiền">
              Tạm tính {{ d.order.subtotal | vnd }}
              @if (d.order.discount > 0) {
                · giảm {{ d.order.discount | vnd }} ({{ d.order.promotionCode }})
              }
              · <strong>tổng {{ d.order.total | vnd }}</strong>
            </nz-descriptions-item>
            @if (d.order.refund; as r) {
              <nz-descriptions-item nzTitle="Hoàn tiền">{{ r.amount | vnd }} — {{ r.status }}</nz-descriptions-item>
            }
          </nz-descriptions>

          <h4 class="section">Giao dịch (đối soát)</h4>
          <nz-table [nzData]="d.payments" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
            <thead><tr><th>Cổng</th><th>Mã giao dịch</th><th nzAlign="right">Số tiền</th><th>Kết quả</th><th>Lúc</th></tr></thead>
            <tbody>
              @for (p of d.payments; track p.providerTxnId) {
                <tr>
                  <td>{{ p.provider }}</td>
                  <td><code>{{ p.providerTxnId }}</code></td>
                  <td nzAlign="right">{{ p.amount | vnd }}</td>
                  <td>{{ p.status }}</td>
                  <td>{{ p.createdAt | date: 'HH:mm:ss dd/MM' : vn }}</td>
                </tr>
              }
            </tbody>
          </nz-table>

          <h4 class="section">Lịch sử thay đổi</h4>
          <nz-timeline>
            @for (e of d.events; track $index) {
              <nz-timeline-item>
                <strong>{{ e.type }}</strong>
                @if (e.toStatus && e.fromStatus !== e.toStatus) {
                  → {{ statusLabels[e.toStatus] }}
                }
                <div class="muted">{{ e.createdAt | date: 'HH:mm:ss dd/MM/yyyy' : vn }}</div>
                @if (e.data) {
                  <code class="data">{{ e.data | json }}</code>
                }
              </nz-timeline-item>
            }
          </nz-timeline>

          @if (canRefund() && (d.order.status === 'PAID' || d.order.status === 'PENDING_PAYMENT')) {
            <button
              nz-button
              nzDanger
              nz-popconfirm
              [nzPopconfirmTitle]="d.order.status === 'PAID' ? 'Hủy đơn và hoàn 100 % cho khách?' : 'Hủy đơn chờ thanh toán?'"
              [nzLoading]="store.busy()"
              (nzOnConfirm)="cancel(d.order.code)"
            >
              {{ d.order.status === 'PAID' ? 'Hủy & hoàn tiền' : 'Hủy đơn' }}
            </button>
          }
        }
      </ng-container>
    </nz-drawer>
  `,
  styles: `
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 16px;
    }

    .w160 {
      width: 160px;
    }

    .w180 {
      width: 180px;
    }

    .w220 {
      width: 220px;
    }

    .row {
      cursor: pointer;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
      font-size: 12px;
    }

    .section {
      margin: 20px 0 8px;
    }

    .data {
      display: block;
      font-size: 11px;
      white-space: pre-wrap;
      word-break: break-all;
    }
  `,
})
export class OrdersPage implements OnInit {
  protected readonly store = inject(OrdersStore);
  private readonly auth = inject(AuthStore);
  private readonly message = inject(NzMessageService);

  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = ORDER_STATUS_LABELS;
  protected readonly statusColors = ORDER_STATUS_COLORS;
  protected readonly ticketLabels = TICKET_STATUS_LABELS;
  protected readonly statusOptions = toOptions(ORDER_STATUS_LABELS);
  protected readonly canRefund = computed(() => this.auth.permissions().has(Permissions.ORDER_REFUND));

  protected q = '';
  protected email = '';
  protected status: OrderStatus | null = null;
  protected range: Date[] = [];

  ngOnInit(): void {
    this.store.search();
  }

  protected search(): void {
    this.store.query = {
      ...this.store.query,
      q: this.q.trim() || null,
      email: this.email.trim() || null,
      status: this.status,
      from: toIsoDate(this.range[0]),
      to: toIsoDate(this.range[1]),
    };
    this.store.search(0);
  }

  protected seatLabels(items: { label: string }[]): string {
    return items.map((i) => i.label).join(', ');
  }

  protected cancel(code: string): void {
    this.store.cancel(code).subscribe(() => this.message.success('Đã hủy đơn'));
  }
}
