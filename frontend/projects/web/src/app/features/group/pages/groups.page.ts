import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { GROUP_STATUS_COLORS, GROUP_STATUS_LABELS, MoviePosterComponent, VN_OFFSET } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { MyGroupsStore } from '../my-groups.store';

/** Nhóm của tôi (GRP20) + nhập mã mời. */
@Component({
  selector: 'app-groups-page',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    NzButtonModule,
    NzCardModule,
    NzEmptyModule,
    NzInputModule,
    NzSkeletonModule,
    NzTagModule,
    MoviePosterComponent,
  ],
  template: `
    <div class="page">
      <div class="head">
        <div>
          <h1>Đặt vé theo nhóm</h1>
          <p class="muted">
            Tạo nhóm, mời bạn bè bằng mã mời, cùng bình chọn suất chiếu — hệ thống tự đề xuất cụm ghế ngồi gần nhau.
          </p>
        </div>
        <a nz-button nzType="primary" nzSize="large" routerLink="/groups/new">Tạo nhóm</a>
      </div>

      <nz-card class="join">
        <form class="join-form" (ngSubmit)="join()">
          <label for="invite-code">Có mã mời?</label>
          <input id="invite-code" nz-input name="code" [(ngModel)]="code" placeholder="Ví dụ: 7KQ2M9XA" maxlength="16" />
          <button nz-button type="submit" [disabled]="!code.trim()">Xem nhóm</button>
        </form>
      </nz-card>

      <h2>Nhóm của tôi</h2>
      @if (store.groups(); as groups) {
        @if (groups.length === 0) {
          <nz-empty nzNotFoundContent="Bạn chưa tham gia nhóm nào" />
        } @else {
          <div class="list">
            @for (g of groups; track g.id) {
              <a class="item" [routerLink]="['/groups', g.id]">
                <lib-movie-poster [url]="g.posterUrl" [title]="g.movieTitle" [width]="56" />
                <div class="info">
                  <div class="name">{{ g.name }}</div>
                  <div class="muted">{{ g.movieTitle }} · {{ g.memberCount }} thành viên</div>
                  <div class="muted">
                    {{ g.myRole === 'LEADER' ? 'Trưởng nhóm' : 'Thành viên' }} · hạn chốt
                    {{ g.rsvpDeadline | date: 'HH:mm dd/MM' : vn }}
                  </div>
                </div>
                <nz-tag [nzColor]="statusColors[g.status]">{{ statusLabels[g.status] }}</nz-tag>
              </a>
            }
          </div>
        }
      } @else {
        <nz-skeleton [nzActive]="true" />
      }
    </div>
  `,
  styles: `
    .page {
      max-width: 860px;
      margin: 0 auto;
    }

    .head {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
    }

    h1 {
      margin-bottom: 4px;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .join {
      margin: 16px 0 24px;
    }

    .join-form {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      input {
        max-width: 220px;
        text-transform: uppercase;
      }
    }

    .list {
      display: grid;
      gap: 12px;
    }

    .item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: #fff;
      border-radius: 8px;
      color: inherit;
    }

    .info {
      flex: 1;
      min-width: 0;
    }

    .name {
      font-size: 16px;
      font-weight: 600;
    }

    @media (max-width: 576px) {
      .head {
        flex-direction: column;
      }
    }
  `,
})
export class GroupsPage implements OnInit {
  protected readonly store = inject(MyGroupsStore);
  private readonly router = inject(Router);

  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = GROUP_STATUS_LABELS;
  protected readonly statusColors = GROUP_STATUS_COLORS;
  protected code = '';

  ngOnInit(): void {
    this.store.loadGroups();
  }

  protected join(): void {
    const code = this.code.trim().toUpperCase();
    if (code) {
      void this.router.navigate(['/groups/join', code]);
    }
  }
}
