import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { VndPipe } from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzMessageService } from 'ng-zorro-antd/message';
import { concatMap, from, last } from 'rxjs';

import { OrderStore } from '../order.store';

/**
 * Cổng thanh toán giả lập (PLAN.md 8.4). Đóng vai cổng thật: người dùng chọn kết quả, trang gửi "IPN" về backend
 * kèm chữ ký backend đã cấp sẵn cho từng kết quả, rồi chuyển về trang kết quả đơn.
 */
@Component({
  selector: 'app-mock-gateway-page',
  imports: [VndPipe, NzCardModule, NzButtonModule, NzAlertModule, NzDescriptionsModule],
  template: `
    <nz-card nzTitle="Cổng thanh toán giả lập">
      <nz-alert
        nzType="warning"
        nzShowIcon
        nzMessage="Trang này chỉ dùng để demo/kiểm thử — không có giao dịch tiền thật."
        class="hint"
      />
      <nz-descriptions nzBordered [nzColumn]="1" nzSize="small">
        <nz-descriptions-item nzTitle="Mã đơn">{{ orderCode() }}</nz-descriptions-item>
        <nz-descriptions-item nzTitle="Mã giao dịch">{{ transactionId() }}</nz-descriptions-item>
        <nz-descriptions-item nzTitle="Nội dung">{{ description() }}</nz-descriptions-item>
        <nz-descriptions-item nzTitle="Số tiền">
          <strong class="amount">{{ +(amount() ?? 0) | vnd }}</strong>
        </nz-descriptions-item>
      </nz-descriptions>
      <div class="actions">
        <button nz-button nzType="primary" nzSize="large" [nzLoading]="store.busy()" (click)="complete(true)">
          Thanh toán thành công
        </button>
        <button nz-button nzDanger nzSize="large" [disabled]="store.busy()" (click)="complete(false)">
          Thanh toán thất bại
        </button>
        <button nz-button nzType="dashed" [disabled]="store.busy()" (click)="complete(true, 2)">
          Thành công + gửi callback 2 lần
        </button>
      </div>
    </nz-card>
  `,
  styles: `
    nz-card {
      max-width: 560px;
      margin: 0 auto;
    }

    .hint {
      margin-bottom: 16px;
    }

    .amount {
      font-size: 18px;
      color: #c0392b;
    }

    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-top: 24px;
    }
  `,
})
export class MockGatewayPage {
  protected readonly store = inject(OrderStore);
  private readonly router = inject(Router);
  private readonly message = inject(NzMessageService);

  // Tham số do backend tạo trong URL chuyển hướng (withComponentInputBinding)
  readonly orderCode = input<string>();
  readonly transactionId = input<string>();
  readonly amount = input<string>();
  readonly description = input<string>();
  readonly sigSuccess = input<string>();
  readonly sigFailed = input<string>();

  /** @param times gửi callback nhiều lần để kiểm tra xử lý trùng (BR05) */
  protected complete(success: boolean, times = 1): void {
    const params: Record<string, string> = {
      orderCode: this.orderCode() ?? '',
      transactionId: this.transactionId() ?? '',
      amount: this.amount() ?? '',
      result: success ? 'SUCCESS' : 'FAILED',
      signature: (success ? this.sigSuccess() : this.sigFailed()) ?? '',
    };
    from(Array.from({ length: times }))
      .pipe(
        concatMap(() => this.store.sendMockCallback(params)),
        last(),
      )
      .subscribe((result) => {
        if (times > 1) {
          this.message.info(`Đã gửi ${times} callback — lần sau được bỏ qua (duplicate = ${result.duplicate})`);
        }
        if (!success) {
          this.message.error('Thanh toán thất bại. Bạn có thể thử lại trong thời gian giữ ghế.');
          void this.router.navigate(['/checkout', result.orderCode], { replaceUrl: true });
          return;
        }
        void this.router.navigate(['/orders', result.orderCode], { replaceUrl: true });
      });
  }
}
