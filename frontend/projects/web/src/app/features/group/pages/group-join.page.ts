import { Component, effect, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { GROUP_STATUS_LABELS, MoviePosterComponent } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';

import { MyGroupsStore } from '../my-groups.store';

/** Tham gia nhóm qua link mời /groups/join/:code (GRP02). Chưa đăng nhập → authGuard đưa tới đăng nhập rồi quay lại. */
@Component({
  selector: 'app-group-join-page',
  imports: [RouterLink, NzButtonModule, NzCardModule, NzResultModule, NzSkeletonModule, MoviePosterComponent],
  template: `
    @if (store.inviteError()) {
      <nz-result nzStatus="404" nzTitle="Mã mời không hợp lệ" nzSubTitle="Mã mời sai hoặc đã được trưởng nhóm tạo lại.">
        <div nz-result-extra><a nz-button nzType="primary" routerLink="/groups">Về trang nhóm</a></div>
      </nz-result>
    }
    @if (store.invite(); as invite) {
      <nz-card class="card">
        <div class="body">
          <lib-movie-poster [url]="invite.posterUrl" [title]="invite.movieTitle" [width]="96" />
          <div>
            <div class="muted">Bạn được mời vào nhóm</div>
            <h2>{{ invite.name }}</h2>
            <div>Phim: <strong>{{ invite.movieTitle }}</strong></div>
            <div class="muted">Trưởng nhóm {{ invite.leaderName }} · {{ invite.memberCount }} thành viên</div>
          </div>
        </div>
        <div class="actions">
          @if (invite.alreadyMember) {
            <a nz-button nzType="primary" [routerLink]="['/groups', invite.id]">Bạn đã ở trong nhóm — mở nhóm</a>
          } @else if (invite.status !== 'OPEN') {
            <span class="muted">Nhóm {{ statusLabels[invite.status].toLowerCase() }}, không nhận thêm thành viên.</span>
          } @else {
            <button nz-button nzType="primary" nzSize="large" [nzLoading]="store.busy()" (click)="join()">
              Tham gia nhóm
            </button>
          }
        </div>
      </nz-card>
    } @else if (!store.inviteError()) {
      <nz-skeleton [nzActive]="true" />
    }
  `,
  styles: `
    .card {
      max-width: 560px;
      margin: 0 auto;
    }

    .body {
      display: flex;
      gap: 16px;
    }

    h2 {
      margin: 4px 0;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .actions {
      margin-top: 24px;
    }
  `,
})
export class GroupJoinPage {
  protected readonly store = inject(MyGroupsStore);
  private readonly router = inject(Router);

  readonly code = input.required<string>();

  protected readonly statusLabels = GROUP_STATUS_LABELS;

  constructor() {
    effect(() => this.store.loadInvite(this.code()));
  }

  protected join(): void {
    this.store.join(this.code()).subscribe((group) => void this.router.navigate(['/groups', group.id]));
  }
}
