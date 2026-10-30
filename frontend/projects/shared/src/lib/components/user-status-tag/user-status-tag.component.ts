import { Component, input } from '@angular/core';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { UserStatus } from '../../models/user.model';

@Component({
  selector: 'lib-user-status-tag',
  imports: [NzTagModule],
  template: `
    @if (status() === 'ACTIVE') {
      <nz-tag nzColor="success">Hoạt động</nz-tag>
    } @else {
      <nz-tag nzColor="error">Đã khóa</nz-tag>
    }
  `,
})
export class UserStatusTagComponent {
  readonly status = input.required<UserStatus>();
}
