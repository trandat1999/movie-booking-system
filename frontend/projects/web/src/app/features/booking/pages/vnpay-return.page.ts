import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSpinModule } from 'ng-zorro-antd/spin';

import { OrderStore } from '../order.store';

/**
 * VNPay chuyển người dùng về đây kèm kết quả đã ký (vnp_*). Trang gửi lại cho backend kiểm tra chữ ký — cùng xử lý
 * idempotent với IPN của VNPay (khi chạy local, IPN không gọi tới được máy dev) — rồi mở trang kết quả đơn.
 */
@Component({
  selector: 'app-vnpay-return-page',
  imports: [RouterLink, NzButtonModule, NzResultModule, NzSpinModule],
  template: `
    @if (error()) {
      <nz-result nzStatus="error" nzTitle="Không xác nhận được kết quả thanh toán" [nzSubTitle]="error()!">
        <div nz-result-extra><a nz-button nzType="primary" routerLink="/account/orders">Đơn hàng của tôi</a></div>
      </nz-result>
    } @else {
      <nz-result nzTitle="Đang xác nhận thanh toán với VNPay…">
        <div nz-result-icon><nz-spin nzSimple nzSize="large" /></div>
      </nz-result>
    }
  `,
})
export class VnpayReturnPage implements OnInit {
  private readonly store = inject(OrderStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const params: Record<string, string> = {};
    const query = this.route.snapshot.queryParamMap;
    query.keys.filter((k) => k.startsWith('vnp_')).forEach((k) => (params[k] = query.get(k) ?? ''));
    if (!params['vnp_TxnRef']) {
      this.error.set('Thiếu thông tin giao dịch.');
      return;
    }
    this.store.sendVnpayReturn(params).subscribe({
      next: (result) => void this.router.navigate(['/orders', result.orderCode], { replaceUrl: true }),
      error: () => this.error.set('Chữ ký không hợp lệ hoặc đơn không tồn tại. Nếu đã bị trừ tiền, vui lòng liên hệ rạp.'),
    });
  }
}
