import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ORDER_STATUS_COLORS, ORDER_STATUS_LABELS, VN_OFFSET, VndPipe } from '@mb/shared';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { MyBookingStore } from '../my-booking.store';

const PAGE_SIZE = 10;

/** Lịch sử đơn hàng (BKG09). */
@Component({
  selector: 'app-my-orders-page',
  imports: [DatePipe, RouterLink, VndPipe, NzCardModule, NzTableModule, NzTagModule],
  template: `
    <nz-card nzTitle="Lịch sử đơn hàng" [nzExtra]="ticketsLink">
      <nz-table
        #table
        [nzData]="store.orders()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.orders()?.totalElements ?? 0"
        [nzPageSize]="pageSize"
        [nzPageIndex]="(store.orders()?.page ?? 0) + 1"
        [nzLoading]="store.loading()"
        nzSize="middle"
        (nzPageIndexChange)="store.loadOrders($event - 1, pageSize)"
      >
        <thead>
          <tr>
            <th>Mã đơn</th>
            <th>Phim / suất chiếu</th>
            <th>Số ghế</th>
            <th nzAlign="right">Tổng tiền</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          @for (o of table.data; track o.code) {
            <tr>
              <td><a [routerLink]="['/orders', o.code]">{{ o.code }}</a></td>
              <td>
                <div class="movie">{{ o.showtime.movieTitle }}</div>
                <div class="muted">
                  {{ o.showtime.startAt | date: 'HH:mm dd/MM/yyyy' : vn }} · {{ o.showtime.cinemaName }}
                </div>
              </td>
              <td>{{ o.seatCount }}</td>
              <td nzAlign="right">{{ o.total | vnd }}</td>
              <td><nz-tag [nzColor]="statusColors[o.status]">{{ statusLabels[o.status] }}</nz-tag></td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>
    <ng-template #ticketsLink><a routerLink="/account/tickets">Vé của tôi</a></ng-template>
  `,
  styles: `
    .movie {
      font-weight: 500;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
      font-size: 13px;
    }
  `,
})
export class MyOrdersPage implements OnInit {
  protected readonly store = inject(MyBookingStore);

  protected readonly pageSize = PAGE_SIZE;
  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = ORDER_STATUS_LABELS;
  protected readonly statusColors = ORDER_STATUS_COLORS;

  ngOnInit(): void {
    this.store.loadOrders(0, PAGE_SIZE);
  }
}
