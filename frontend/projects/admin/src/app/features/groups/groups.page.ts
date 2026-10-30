import { DatePipe, JsonPipe } from '@angular/common';
import { Component, inject, Injectable, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AdminGroupDetail,
  AdminGroupRow,
  AdminReportApi,
  GROUP_STATUS_COLORS,
  GROUP_STATUS_LABELS,
  GroupStatus,
  PageResponse,
  RSVP_LABELS,
  toOptions,
  VN_OFFSET,
  VndPipe,
} from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTimelineModule } from 'ng-zorro-antd/timeline';
import { finalize } from 'rxjs';

/** Danh sách & chi tiết nhóm cho quản trị (GRPADM). */
@Injectable()
export class GroupsStore {
  private readonly api = inject(AdminReportApi);

  readonly page = signal<PageResponse<AdminGroupRow> | null>(null);
  readonly detail = signal<AdminGroupDetail | null>(null);
  readonly loading = signal(false);

  status: GroupStatus | null = null;
  q = '';

  search(page = 0): void {
    this.loading.set(true);
    this.api
      .groups(this.status, this.q.trim() || null, page, 20)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => this.page.set(result));
  }

  open(id: number): void {
    this.detail.set(null);
    this.api.group(id).subscribe((detail) => this.detail.set(detail));
  }

  close(): void {
    this.detail.set(null);
  }
}

@Component({
  selector: 'adm-groups-page',
  providers: [GroupsStore],
  imports: [
    DatePipe,
    JsonPipe,
    FormsModule,
    VndPipe,
    NzButtonModule,
    NzCardModule,
    NzDescriptionsModule,
    NzDrawerModule,
    NzInputModule,
    NzSelectModule,
    NzTableModule,
    NzTagModule,
    NzTimelineModule,
  ],
  template: `
    <nz-card nzTitle="Nhóm đặt vé">
      <form class="filters" (ngSubmit)="store.search(0)">
        <input nz-input name="q" [(ngModel)]="store.q" placeholder="Tên nhóm hoặc mã mời" aria-label="Tìm nhóm" class="w240" />
        <nz-select name="status" [(ngModel)]="store.status" nzAllowClear nzPlaceHolder="Mọi trạng thái" class="w200" aria-label="Trạng thái nhóm">
          @for (s of statusOptions; track s.value) {
            <nz-option [nzValue]="s.value" [nzLabel]="s.label" />
          }
        </nz-select>
        <button nz-button nzType="primary" type="submit">Tìm</button>
      </form>
      <nz-table
        #table
        [nzData]="store.page()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.page()?.totalElements ?? 0"
        [nzPageIndex]="(store.page()?.page ?? 0) + 1"
        [nzPageSize]="20"
        [nzLoading]="store.loading()"
        (nzPageIndexChange)="store.search($event - 1)"
        [nzScroll]="{ x: '900px' }"
      >
        <thead>
          <tr>
            <th>Nhóm</th>
            <th>Phim</th>
            <th>Trưởng nhóm</th>
            <th>Thành viên</th>
            <th>Số vé chốt</th>
            <th>Trạng thái</th>
            <th>Tạo lúc</th>
          </tr>
        </thead>
        <tbody>
          @for (g of table.data; track g.id) {
            <tr class="row" tabindex="0" (click)="store.open(g.id)" (keyup.enter)="store.open(g.id)">
              <td>{{ g.name }} <code class="muted">{{ g.code }}</code></td>
              <td>{{ g.movieTitle }}</td>
              <td>{{ g.leader?.fullName }}</td>
              <td>{{ g.memberCount }}</td>
              <td>{{ g.lockedTicketCount ?? '—' }}</td>
              <td><nz-tag [nzColor]="statusColors[g.status]">{{ statusLabels[g.status] }}</nz-tag></td>
              <td>{{ g.createdAt | date: 'HH:mm dd/MM/yyyy' : vn }}</td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>

    <nz-drawer [nzVisible]="store.detail() !== null" [nzWidth]="600" nzTitle="Chi tiết nhóm" (nzOnClose)="store.close()">
      <ng-container *nzDrawerContent>
        @if (store.detail(); as d) {
          <nz-descriptions [nzColumn]="1" nzSize="small" nzBordered>
            <nz-descriptions-item nzTitle="Nhóm">{{ d.group.name }} (<code>{{ d.group.code }}</code>)</nz-descriptions-item>
            <nz-descriptions-item nzTitle="Trạng thái">
              <nz-tag [nzColor]="statusColors[d.group.status]">{{ statusLabels[d.group.status] }}</nz-tag>
            </nz-descriptions-item>
            <nz-descriptions-item nzTitle="Phim">{{ d.group.movie.title }}</nz-descriptions-item>
            <nz-descriptions-item nzTitle="Hạn chốt">{{ d.group.rsvpDeadline | date: 'HH:mm dd/MM/yyyy' : vn }}</nz-descriptions-item>
            @if (d.group.locked; as l) {
              <nz-descriptions-item nzTitle="Suất đã chốt">
                {{ l.showtime.startAt | date: 'HH:mm dd/MM/yyyy' : vn }} · {{ l.showtime.cinemaName }} · {{ l.showtime.roomName }} —
                {{ l.ticketCount }} vé
              </nz-descriptions-item>
            }
            @if (d.group.order; as o) {
              <nz-descriptions-item nzTitle="Đơn">{{ o.code }} — {{ o.total | vnd }} ({{ o.status }})</nz-descriptions-item>
            }
          </nz-descriptions>

          <h4 class="section">Thành viên</h4>
          <nz-table [nzData]="d.group.members" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
            <tbody>
              @for (m of d.group.members; track m.userId) {
                <tr>
                  <td>{{ m.fullName }} @if (m.role === 'LEADER') {<nz-tag nzColor="purple">Trưởng nhóm</nz-tag>}</td>
                  <td>{{ rsvpLabels[m.rsvp] }}{{ m.guestCount ? ' +' + m.guestCount + ' khách' : '' }}</td>
                </tr>
              }
            </tbody>
          </nz-table>

          @if (d.group.assignments.length) {
            <h4 class="section">Ghế của nhóm</h4>
            <div>
              @for (a of d.group.assignments; track a.ticketCode) {
                <nz-tag>{{ a.seatLabel }} — {{ a.holderName }}</nz-tag>
              }
            </div>
          }

          <h4 class="section">Đề xuất cụm ghế ({{ d.suggestions.length }})</h4>
          <nz-table [nzData]="d.suggestions" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
            <thead><tr><th>Lúc</th><th>Số vé</th><th>Phương án</th><th>Liền kề</th><th>Chọn</th><th>ms</th></tr></thead>
            <tbody>
              @for (s of d.suggestions; track s.id) {
                <tr>
                  <td>{{ s.createdAt | date: 'HH:mm dd/MM' : vn }}</td>
                  <td>{{ s.partySize }}</td>
                  <td>{{ s.reason ?? s.optionCount }}</td>
                  <td>{{ s.contiguous ? 'Có' : 'Không' }}</td>
                  <td>{{ s.chosenRank === null ? '—' : s.chosenRank === 0 ? 'Tự chọn' : '#' + s.chosenRank }}</td>
                  <td>{{ s.durationMs }}</td>
                </tr>
              }
            </tbody>
          </nz-table>

          <h4 class="section">Nhật ký nhóm</h4>
          <nz-timeline>
            @for (e of d.events; track $index) {
              <nz-timeline-item>
                <strong>{{ e.type }}</strong> {{ e.actorName ? '— ' + e.actorName : '' }}
                <div class="muted">{{ e.createdAt | date: 'HH:mm:ss dd/MM/yyyy' : vn }}</div>
                @if (e.data && hasKeys(e.data)) {
                  <code class="data">{{ e.data | json }}</code>
                }
              </nz-timeline-item>
            }
          </nz-timeline>
        }
      </ng-container>
    </nz-drawer>
  `,
  styles: `
    .filters {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 16px;
    }

    .w200 {
      width: 200px;
    }

    .w240 {
      width: 240px;
    }

    .row {
      cursor: pointer;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
      font-size: 12px;
    }

    .section {
      margin: 20px 0 8px;
    }

    .data {
      display: block;
      font-size: 11px;
      white-space: pre-wrap;
      word-break: break-all;
    }
  `,
})
export class GroupsPage implements OnInit {
  protected readonly store = inject(GroupsStore);

  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = GROUP_STATUS_LABELS;
  protected readonly statusColors = GROUP_STATUS_COLORS;
  protected readonly rsvpLabels = RSVP_LABELS;
  protected readonly statusOptions = toOptions(GROUP_STATUS_LABELS);

  ngOnInit(): void {
    this.store.search();
  }

  protected hasKeys(data: Record<string, unknown>): boolean {
    return Object.keys(data).length > 0;
  }
}
