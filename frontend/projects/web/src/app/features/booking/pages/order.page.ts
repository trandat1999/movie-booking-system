import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ORDER_STATUS_COLORS, ORDER_STATUS_LABELS, VN_OFFSET, VndPipe } from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
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
    NzAlertModule,
    NzModalModule,
    VndPipe,
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
            <nz-result nzStatus="info" [nzTitle]="order.refund ? 'Đơn hàng đã hủy và hoàn tiền' : 'Đơn hàng đã hủy'">
              <div nz-result-extra>
                <a nz-button nzType="primary" [routerLink]="['/showtimes', order.showtime.id]">Chọn lại ghế</a>
              </div>
            </nz-result>
          }
          @case ('REFUND') {
            <nz-result nzStatus="warning" nzTitle="Đơn hàng sẽ được hoàn tiền" nzSubTitle="Thanh toán đến sau khi hết thời gian giữ ghế và ghế đã được bán cho người khác. Chúng tôi sẽ hoàn tiền cho bạn." />
          }
        }

        @if (order.refund; as refund) {
          <nz-alert
            class="block"
            nzShowIcon
            [nzType]="refund.status === 'COMPLETED' ? 'success' : 'info'"
            [nzMessage]="
              refund.status === 'COMPLETED'
                ? 'Đã hoàn ' + (refund.amount | vnd) + ' về phương thức thanh toán ban đầu.'
                : 'Đang hoàn ' + (refund.amount | vnd) + ' — rạp sẽ xử lý trong 1–3 ngày làm việc.'
            "
          />
        }

        @if (order.status === 'PAID') {
          @if (order.qrImage && order.tickets.length > 1) {
            <nz-card nzTitle="QR cả đơn" class="block">
              <div class="order-qr">
                <img [src]="order.qrImage" [alt]="'Mã QR đơn ' + order.code" width="160" height="160" />
                <p class="muted">Cả nhóm vào rạp cùng lúc: nhân viên quét mã này để soát toàn bộ {{ order.tickets.length }} vé.</p>
              </div>
            </nz-card>
          }
          <div class="block cancel">
            @if (order.groupId) {
              <span class="muted">Đơn của nhóm — trưởng nhóm hủy từ <a [routerLink]="['/groups', order.groupId]">trang nhóm</a>.</span>
            } @else if (canCancel()) {
              <span class="muted">Được hủy và hoàn tiền theo chính sách trước {{ order.cancellableUntil | date: 'HH:mm dd/MM/yyyy' : vn }}.</span>
              <button nz-button nzDanger [nzLoading]="store.busy()" (click)="cancel()">Hủy vé</button>
            } @else {
              <span class="muted">Đã quá thời hạn tự hủy vé.</span>
            }
          </div>
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

    .order-qr {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .cancel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      flex-wrap: wrap;
    }

    .meta {
      margin: 12px 0 0;
    }
  `,
})
export class OrderPage {
  protected readonly store = inject(OrderStore);
  private readonly modal = inject(NzModalService);

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
      case 'REFUNDED':
        return 'CANCELLED';
      default:
        return 'REFUND';
    }
  });

  /** Còn trong hạn tự hủy (so với giờ máy chủ). */
  protected readonly canCancel = computed(() => {
    const order = this.store.order();
    return !!order?.cancellableUntil && new Date(order.serverTime) < new Date(order.cancellableUntil);
  });

  constructor() {
    effect(() => {
      this.store.load(this.code());
      this.store.watchUntilSettled(this.code());
    });
  }

  protected cancel(): void {
    this.modal.confirm({
      nzTitle: 'Hủy vé và hoàn tiền?',
      nzContent: 'Toàn bộ vé của đơn bị hủy, ghế được mở bán lại. Tiền hoàn theo chính sách của rạp.',
      nzOkText: 'Hủy vé',
      nzOkDanger: true,
      nzCancelText: 'Không',
      nzOnOk: () => this.store.cancel().subscribe(),
    });
  }
}
