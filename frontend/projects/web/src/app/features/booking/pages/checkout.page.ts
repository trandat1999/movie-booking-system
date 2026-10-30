import { Component, effect, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { FormsModule } from '@angular/forms';

import { CountdownComponent } from '../../../shared/components/countdown.component';
import { OrderSummaryComponent } from '../components/order-summary.component';
import { OrderStore } from '../order.store';

/** Xác nhận đơn và chọn phương thức thanh toán trong thời gian giữ ghế (BKG05, BKG06). */
@Component({
  selector: 'app-checkout-page',
  imports: [
    FormsModule,
    RouterLink,
    NzCardModule,
    NzButtonModule,
    NzAlertModule,
    NzModalModule,
    NzRadioModule,
    NzResultModule,
    NzSkeletonModule,
    CountdownComponent,
    OrderSummaryComponent,
  ],
  template: `
    @if (store.notFound()) {
      <nz-result nzStatus="404" nzTitle="Không tìm thấy đơn hàng">
        <div nz-result-extra><a nz-button nzType="primary" routerLink="/movies">Chọn phim</a></div>
      </nz-result>
    }
    @if (store.order(); as order) {
      <nz-card nzTitle="Thanh toán" [nzExtra]="timer">
        <nz-alert
          nzType="info"
          nzShowIcon
          nzMessage="Ghế đang được giữ cho bạn. Vui lòng thanh toán trước khi hết thời gian, sau đó ghế sẽ được trả lại."
          class="hint"
        />
        <app-order-summary [order]="order" />

        <h3 class="section">Phương thức thanh toán</h3>
        <!-- label[nz-radio] tự chứa ô input bên trong nên luật a11y không nhận ra -->
        <!-- eslint-disable @angular-eslint/template/label-has-associated-control -->
        <nz-radio-group [(ngModel)]="provider" class="providers">
          @for (p of store.providers(); track p.code) {
            <label nz-radio [nzValue]="p.code">{{ p.name }}</label>
          }
        </nz-radio-group>
        <!-- eslint-enable @angular-eslint/template/label-has-associated-control -->

        <div class="actions">
          <button nz-button nzDanger [disabled]="store.busy()" (click)="confirmCancel()">Hủy đơn</button>
          <button nz-button nzType="primary" nzSize="large" [nzLoading]="store.busy()" (click)="pay()">
            Thanh toán
          </button>
        </div>
      </nz-card>
      <ng-template #timer>
        Còn lại <app-countdown [expiresAt]="order.expiresAt" [serverTime]="order.serverTime" (expired)="onExpired()" />
      </ng-template>
    } @else if (!store.notFound()) {
      <nz-skeleton [nzActive]="true" [nzParagraph]="{ rows: 8 }" />
    }
  `,
  styles: `
    nz-card {
      max-width: 760px;
      margin: 0 auto;
    }

    .hint {
      margin-bottom: 16px;
    }

    .section {
      margin: 24px 0 8px;
    }

    .providers {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .actions {
      display: flex;
      justify-content: space-between;
      margin-top: 24px;
    }
  `,
})
export class CheckoutPage {
  protected readonly store = inject(OrderStore);
  private readonly router = inject(Router);
  private readonly modal = inject(NzModalService);

  readonly code = input.required<string>();

  protected provider = 'MOCK';

  constructor() {
    effect(() => this.store.load(this.code()));
    this.store.loadProviders();
    // Đơn không còn chờ thanh toán (đã trả, hết hạn, hủy) → sang trang kết quả
    effect(() => {
      const status = this.store.status();
      if (status && status !== 'PENDING_PAYMENT') {
        void this.router.navigate(['/orders', this.code()], { replaceUrl: true });
      }
    });
  }

  protected pay(): void {
    this.store.pay(this.provider).subscribe((url) => {
      if (url.startsWith('/')) {
        void this.router.navigateByUrl(url);
      } else {
        window.location.assign(url);
      }
    });
  }

  protected confirmCancel(): void {
    this.modal.confirm({
      nzTitle: 'Hủy đơn hàng?',
      nzContent: 'Ghế đang giữ sẽ được trả lại cho người khác.',
      nzOkText: 'Hủy đơn',
      nzOkDanger: true,
      nzCancelText: 'Không',
      nzOnOk: () => this.store.cancel().subscribe(),
    });
  }

  protected onExpired(): void {
    void this.router.navigate(['/orders', this.code()], { replaceUrl: true });
  }
}
