import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ORDER_STATUS_COLORS, ORDER_STATUS_LABELS, VN_OFFSET } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { TicketCardComponent } from '../../../shared/components/ticket-card.component';
import { OrderSummaryComponent } from '../components/order-summary.component';
import { OrderStore } from '../order.store';

/** Kết quả đơn hàng sau thanh toán và vé điện tử (BKG07, BKG08). */
@Component({
  selector: 'app-order-page',
  imports: [
    DatePipe,
    RouterLink,
    NzCardModule,
    NzButtonModule,
    NzResultModule,
    NzSkeletonModule,
    NzSpinModule,
    NzTagModule,
    OrderSummaryComponent,
    TicketCardComponent,
  ],
  template: `
    @if (store.notFound()) {
      <nz-result nzStatus="404" nzTitle="Không tìm thấy đơn hàng">
        <div nz-result-extra><a nz-button nzType="primary" routerLink="/account/orders">Đơn hàng của tôi</a></div>
      </nz-result>
    }
    @if (store.order(); as order) {
      <div class="page">
        @switch (view()) {
          @case ('PAID') {
            <nz-result nzStatus="success" nzTitle="Đặt vé thành công!" [nzSubTitle]="'Mã đơn ' + order.code + ' — vé điện tử đã được gửi tới email của bạn.'" />
          }
          @case ('PENDING') {
            <nz-result nzTitle="Đang chờ thanh toán" nzSubTitle="Trang sẽ tự cập nhật khi nhận được kết quả từ cổng thanh toán.">
              <div nz-result-icon><nz-spin nzSimple nzSize="large" /></div>
              <div nz-result-extra>
                <a nz-button nzType="primary" [routerLink]="['/checkout', order.code]">Tiếp tục thanh toán</a>
              </div>
            </nz-result>
          }
          @case ('EXPIRED') {
            <nz-result nzStatus="warning" nzTitle="Đơn hàng đã hết hạn" nzSubTitle="Đã hết thời gian giữ ghế. Bạn có thể chọn lại ghế cho suất chiếu này.">
              <div nz-result-extra>
                <a nz-button nzType="primary" [routerLink]="['/showtimes', order.showtime.id]">Chọn lại ghế</a>
              </div>
            </nz-result>
          }
          @case ('CANCELLED') {
            <nz-result nzStatus="info" nzTitle="Đơn hàng đã hủy">
              <div nz-result-extra>
                <a nz-button nzType="primary" [routerLink]="['/showtimes', order.showtime.id]">Chọn lại ghế</a>
              </div>
            </nz-result>
          }
          @case ('REFUND') {
            <nz-result nzStatus="warning" nzTitle="Đơn hàng sẽ được hoàn tiền" nzSubTitle="Thanh toán đến sau khi hết thời gian giữ ghế và ghế đã được bán cho người khác. Chúng tôi sẽ hoàn tiền cho bạn." />
          }
        }

        @if (order.tickets.length > 0) {
          <nz-card nzTitle="Vé điện tử" class="block">
            <p class="muted">Xuất trình mã QR tại quầy soát vé. Mỗi mã chỉ dùng được một lần.</p>
            <div class="tickets">
              @for (t of order.tickets; track t.code) {
                <app-ticket-card [code]="t.code" [seatLabel]="t.seatLabel" [status]="t.status" [qrImage]="t.qrImage" [showtime]="order.showtime" />
              }
            </div>
          </nz-card>
        }

        <nz-card [nzTitle]="'Đơn ' + order.code" [nzExtra]="statusTag" class="block">
          <app-order-summary [order]="order" />
          <p class="muted meta">
            Đặt lúc {{ order.createdAt | date: 'HH:mm dd/MM/yyyy' : vn }}
            @if (order.paidAt) {
              · Thanh toán lúc {{ order.paidAt | date: 'HH:mm dd/MM/yyyy' : vn }}
            }
          </p>
        </nz-card>
        <ng-template #statusTag>
          <nz-tag [nzColor]="statusColors[order.status]">{{ statusLabels[order.status] }}</nz-tag>
        </ng-template>
      </div>
    } @else if (!store.notFound()) {
      <nz-skeleton [nzActive]="true" [nzParagraph]="{ rows: 8 }" />
    }
  `,
  styles: `
    .page {
      max-width: 760px;
      margin: 0 auto;
    }

    .block {
      margin-bottom: 16px;
    }

    .tickets {
      display: grid;
      gap: 12px;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .meta {
      margin: 12px 0 0;
    }
  `,
})
export class OrderPage {
  protected readonly store = inject(OrderStore);

  readonly code = input.required<string>();

  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = ORDER_STATUS_LABELS;
  protected readonly statusColors = ORDER_STATUS_COLORS;

  /** Đơn chờ thanh toán nhưng đã quá hạn giữ ghế (job dọn chưa chạy) cũng hiển thị là hết hạn. */
  protected readonly view = computed(() => {
    const order = this.store.order();
    switch (order?.status) {
      case 'PAID':
      case 'COMPLETED':
        return 'PAID';
      case 'PENDING_PAYMENT':
        return new Date(order.expiresAt) <= new Date(order.serverTime) ? 'EXPIRED' : 'PENDING';
      case 'EXPIRED':
        return 'EXPIRED';
      case 'CANCELLED':
        return 'CANCELLED';
      default:
        return 'REFUND';
    }
  });

  constructor() {
    effect(() => {
      this.store.load(this.code());
      this.store.watchUntilSettled(this.code());
    });
  }
}
