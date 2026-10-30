import { Component } from '@angular/core';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';

/** Dashboard quản trị — các chỉ số ADM01/ADM02 được xây dựng ở M8. */
@Component({
  selector: 'adm-dashboard-page',
  imports: [NzCardModule, NzEmptyModule],
  template: `
    <nz-card nzTitle="Dashboard">
      <nz-empty nzNotFoundContent="Chưa có dữ liệu thống kê" />
    </nz-card>
  `,
})
export class DashboardPage {}
