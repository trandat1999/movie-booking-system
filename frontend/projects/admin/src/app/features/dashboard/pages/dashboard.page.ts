import { DecimalPipe, PercentPipe } from '@angular/common';
import { Component, computed, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthStore, Permissions, StatsFilter, toIsoDate, VndPipe } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzStatisticModule } from 'ng-zorro-antd/statistic';
import { NzTableModule } from 'ng-zorro-antd/table';

import { BarChartComponent, BarPoint } from '../../../shared/bar-chart.component';
import { DashboardStore } from '../dashboard.store';

const vndShort = (v: number): string =>
  v >= 1_000_000 ? `${(v / 1_000_000).toFixed(v >= 10_000_000 ? 0 : 1)} tr` : v >= 1000 ? `${Math.round(v / 1000)} k` : String(v);

/** Dashboard quản trị: ADM01 (tổng quan), ADM02 (nhóm), ADM03 (lọc + xuất CSV). */
@Component({
  selector: 'adm-dashboard-page',
  providers: [DashboardStore],
  imports: [
    DecimalPipe,
    PercentPipe,
    FormsModule,
    VndPipe,
    NzButtonModule,
    NzCardModule,
    NzDatePickerModule,
    NzEmptyModule,
    NzGridModule,
    NzSelectModule,
    NzSpinModule,
    NzStatisticModule,
    NzTableModule,
    BarChartComponent,
  ],
  template: `
    @if (!canView()) {
      <nz-card nzTitle="Dashboard">
        <nz-empty nzNotFoundContent="Bạn không có quyền xem thống kê" />
      </nz-card>
    } @else {
      <nz-card class="filters">
        <div class="filter-row">
          <nz-range-picker [(ngModel)]="range" nzFormat="dd/MM/yyyy" [nzAllowClear]="false" aria-label="Khoảng ngày" />
          <nz-select [(ngModel)]="cinemaId" nzAllowClear nzPlaceHolder="Tất cả rạp" class="select" aria-label="Rạp">
            @for (c of store.cinemas(); track c.id) {
              <nz-option [nzValue]="c.id" [nzLabel]="c.name" />
            }
          </nz-select>
          <nz-select [(ngModel)]="movieId" nzAllowClear nzShowSearch nzPlaceHolder="Tất cả phim" class="select" aria-label="Phim">
            @for (m of store.movies(); track m.id) {
              <nz-option [nzValue]="m.id" [nzLabel]="m.title" />
            }
          </nz-select>
          <button nz-button nzType="primary" [nzLoading]="store.loading()" (click)="reload()">Xem</button>
          <button nz-button [nzLoading]="store.exporting()" (click)="store.exportCsv(filter())">Xuất CSV đơn hàng</button>
        </div>
      </nz-card>

      <nz-spin [nzSpinning]="store.loading()">
        @if (store.overview(); as o) {
          <div class="kpis">
            <nz-card><nz-statistic nzTitle="Doanh thu" [nzValue]="(o.revenue | vnd) ?? ''" /></nz-card>
            <nz-card><nz-statistic nzTitle="Vé đã bán" [nzValue]="o.ticketsSold" /></nz-card>
            <nz-card><nz-statistic nzTitle="Đơn đã thanh toán" [nzValue]="o.orders" /></nz-card>
            <nz-card><nz-statistic nzTitle="Giá trị đơn trung bình" [nzValue]="(o.avgOrderValue | vnd) ?? ''" /></nz-card>
            <nz-card>
              <nz-statistic nzTitle="Tỷ lệ lấp đầy" [nzValue]="(o.occupancyRate | percent: '1.0-1') ?? ''" />
              <div class="hint">{{ o.seatsSold }}/{{ o.seatsTotal }} ghế (suất trong khoảng)</div>
            </nz-card>
            <nz-card>
              <nz-statistic nzTitle="Thanh toán thất bại" [nzValue]="(o.paymentFailureRate | percent: '1.0-1') ?? ''" />
              <div class="hint">{{ o.paymentFailed }}/{{ o.paymentAttempts }} giao dịch · {{ o.expiredOrders }} đơn hết hạn</div>
            </nz-card>
            <nz-card>
              <nz-statistic nzTitle="Đã hoàn tiền" [nzValue]="(o.refundTotal | vnd) ?? ''" />
              <div class="hint">{{ o.refundCount }} đơn · giảm giá {{ o.discountTotal | vnd }}</div>
            </nz-card>
          </div>

          <nz-card nzTitle="Doanh thu theo ngày" class="block">
            <adm-bar-chart [points]="dailyRevenue()" [format]="vndShort" ariaLabel="Doanh thu theo ngày" />
          </nz-card>

          <div class="two">
            <nz-card nzTitle="Phim doanh thu cao nhất">
              <nz-table [nzData]="o.topMovies" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
                <thead><tr><th>Phim</th><th nzAlign="right">Vé</th><th nzAlign="right">Doanh thu</th></tr></thead>
                <tbody>
                  @for (m of o.topMovies; track m.id) {
                    <tr><td>{{ m.name }}</td><td nzAlign="right">{{ m.tickets }}</td><td nzAlign="right">{{ m.revenue | vnd }}</td></tr>
                  }
                </tbody>
              </nz-table>
            </nz-card>
            <nz-card nzTitle="Theo rạp">
              <nz-table [nzData]="o.cinemas" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
                <thead><tr><th>Rạp</th><th nzAlign="right">Đơn</th><th nzAlign="right">Doanh thu</th></tr></thead>
                <tbody>
                  @for (c of o.cinemas; track c.id) {
                    <tr><td>{{ c.name }}</td><td nzAlign="right">{{ c.orders }}</td><td nzAlign="right">{{ c.revenue | vnd }}</td></tr>
                  }
                </tbody>
              </nz-table>
            </nz-card>
          </div>
        }

        @if (store.groups(); as g) {
          <nz-card nzTitle="Đặt vé theo nhóm" class="block">
            <div class="kpis">
              <nz-statistic nzTitle="Nhóm tạo mới" [nzValue]="g.totalGroups" />
              <nz-statistic nzTitle="Tạo → đặt thành công" [nzValue]="(g.conversionRate | percent: '1.0-1') ?? ''" />
              <nz-statistic nzTitle="Số vé TB / nhóm đã đặt" [nzValue]="(g.avgTickets | number: '1.0-1') ?? ''" />
              <nz-statistic nzTitle="Thành viên TB / nhóm" [nzValue]="(g.avgMembers | number: '1.0-1') ?? ''" />
              <nz-statistic
                nzTitle="Thời gian tạo → đặt (giờ)"
                [nzValue]="g.avgHoursToBook === null ? '—' : ((g.avgHoursToBook | number: '1.0-1') ?? '')"
              />
              <nz-statistic nzTitle="Tìm được cụm liền kề" [nzValue]="(g.suggestions.contiguousRate | percent: '1.0-1') ?? ''" />
            </div>
            <div class="two">
              <div>
                <h4>Thuật toán cụm ghế</h4>
                <ul class="facts">
                  <li>{{ g.suggestions.count }} lần đề xuất, trung bình {{ g.suggestions.avgDurationMs | number: '1.0-1' }} ms</li>
                  <li>{{ g.suggestions.noCluster }} lần không có phương án phù hợp</li>
                  <li>
                    Khi giữ ghế: {{ g.suggestions.topChoiceRate | percent: '1.0-0' }} chọn phương án #1,
                    {{ g.suggestions.manualRate | percent: '1.0-0' }} tự chọn tay ({{ g.suggestions.holds }} lần)
                  </li>
                </ul>
                <h4>Quy mô nhóm đã đặt</h4>
                <adm-bar-chart [points]="sizePoints()" color="#52c41a" ariaLabel="Quy mô nhóm" />
              </div>
              <div>
                <h4>Lý do không thành công</h4>
                <nz-table [nzData]="g.failures" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
                  <tbody>
                    @for (f of g.failures; track f.label) {
                      <tr><td>{{ f.label }}</td><td nzAlign="right">{{ f.count }}</td></tr>
                    }
                  </tbody>
                </nz-table>
              </div>
            </div>
          </nz-card>
        }
      </nz-spin>
    }
  `,
  styles: `
    .filters,
    .block {
      margin-bottom: 16px;
    }

    .filter-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .select {
      min-width: 200px;
    }

    .kpis {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
      gap: 12px;
      margin-bottom: 16px;
    }

    .hint {
      margin-top: 4px;
      color: rgba(0, 0, 0, 0.45);
      font-size: 12px;
    }

    .two {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 16px;
    }

    .facts {
      padding-left: 18px;
    }

    @media (max-width: 900px) {
      .two {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class DashboardPage implements OnInit {
  protected readonly store = inject(DashboardStore);
  private readonly auth = inject(AuthStore);

  protected readonly vndShort = vndShort;
  protected readonly canView = computed(() => this.auth.permissions().has(Permissions.STATS_READ));
  protected range: Date[] = defaultRange();
  protected cinemaId: number | null = null;
  protected movieId: number | null = null;

  protected readonly dailyRevenue = computed<BarPoint[]>(() =>
    (this.store.overview()?.daily ?? []).map((d) => ({
      label: d.date.slice(8, 10) + '/' + d.date.slice(5, 7),
      value: d.revenue,
      title: `${d.date}: ${d.revenue.toLocaleString('vi-VN')} ₫ · ${d.orders} đơn · ${d.tickets} vé`,
    })),
  );

  protected readonly sizePoints = computed<BarPoint[]>(() =>
    (this.store.groups()?.sizeBuckets ?? []).map((b) => ({ label: b.label, value: b.count })),
  );

  ngOnInit(): void {
    if (this.canView()) {
      this.store.loadOptions();
      this.reload();
    }
  }

  protected filter(): StatsFilter {
    return {
      from: toIsoDate(this.range[0]),
      to: toIsoDate(this.range[1]),
      cinemaId: this.cinemaId,
      movieId: this.movieId,
    };
  }

  protected reload(): void {
    this.store.load(this.filter());
  }
}

function defaultRange(): Date[] {
  const to = new Date();
  const from = new Date();
  from.setDate(to.getDate() - 29);
  return [from, to];
}
