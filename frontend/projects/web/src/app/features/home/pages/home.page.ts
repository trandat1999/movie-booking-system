import { Component } from '@angular/core';
import { NzResultModule } from 'ng-zorro-antd/result';

/** Trang chủ — nội dung phim đang chiếu / sắp chiếu được xây dựng ở M3 (MOV01). */
@Component({
  selector: 'app-home-page',
  imports: [NzResultModule],
  template: `
    <nz-result
      nzStatus="info"
      nzTitle="Hệ thống bán vé xem phim & đặt vị trí theo nhóm"
      nzSubTitle="Khung dự án đã sẵn sàng. Danh sách phim sẽ hiển thị tại đây."
    />
  `,
})
export class HomePage {}
