import { Component, computed, input } from '@angular/core';

export interface BarPoint {
  label: string;
  value: number;
  /** Chú thích khi rê chuột */
  title?: string;
}

/** Biểu đồ cột SVG đơn giản (không cần thư viện biểu đồ), co giãn theo bề ngang. */
@Component({
  selector: 'adm-bar-chart',
  template: `
    @if (points().length === 0) {
      <div class="empty">Chưa có dữ liệu</div>
    } @else {
      <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" class="chart" role="img" [attr.aria-label]="ariaLabel()">
        @for (tick of ticks(); track tick.y) {
          <line [attr.x1]="left" [attr.x2]="width" [attr.y1]="tick.y" [attr.y2]="tick.y" class="grid" />
          <text [attr.x]="left - 6" [attr.y]="tick.y + 4" text-anchor="end" class="axis">{{ tick.label }}</text>
        }
        @for (bar of bars(); track bar.x) {
          <rect [attr.x]="bar.x" [attr.y]="bar.y" [attr.width]="bar.w" [attr.height]="bar.h" [attr.fill]="color()" rx="2">
            <title>{{ bar.title }}</title>
          </rect>
          @if (bar.showLabel) {
            <text [attr.x]="bar.x + bar.w / 2" [attr.y]="height - 6" text-anchor="middle" class="axis">{{ bar.label }}</text>
          }
        }
      </svg>
    }
  `,
  styles: `
    .chart {
      width: 100%;
      height: auto;
    }

    .grid {
      stroke: #f0f0f0;
    }

    .axis {
      font-size: 11px;
      fill: #8c8c8c;
    }

    .empty {
      padding: 40px 0;
      text-align: center;
      color: #8c8c8c;
    }
  `,
})
export class BarChartComponent {
  readonly points = input.required<BarPoint[]>();
  readonly color = input('#1677ff');
  readonly ariaLabel = input('Biểu đồ cột');
  /** Định dạng nhãn trục tung */
  readonly format = input<(v: number) => string>((v) => String(v));

  protected readonly width = 720;
  protected readonly height = 240;
  protected readonly left = 64;
  private readonly bottom = 24;
  private readonly top = 8;

  private readonly max = computed(() => {
    const max = Math.max(...this.points().map((p) => p.value), 0);
    return max === 0 ? 1 : niceCeil(max);
  });

  protected readonly ticks = computed(() => {
    const plot = this.height - this.top - this.bottom;
    return [0, 0.25, 0.5, 0.75, 1].map((t) => ({
      y: this.top + plot - plot * t,
      label: this.format()(Math.round(this.max() * t)),
    }));
  });

  protected readonly bars = computed(() => {
    const points = this.points();
    const plotW = this.width - this.left;
    const plotH = this.height - this.top - this.bottom;
    const slot = plotW / points.length;
    const w = Math.min(48, Math.max(2, slot * 0.7));
    // Nhiều cột thì chỉ hiện nhãn cách quãng cho dễ đọc
    const every = Math.ceil(points.length / 12);
    return points.map((p, i) => {
      const h = (plotH * p.value) / this.max();
      return {
        x: this.left + slot * i + (slot - w) / 2,
        y: this.top + plotH - h,
        w,
        h,
        label: p.label,
        title: p.title ?? `${p.label}: ${p.value}`,
        showLabel: i % every === 0,
      };
    });
  });
}

/** Làm tròn lên số "đẹp" (1, 2, 5 × 10^n) cho đỉnh trục. */
function niceCeil(value: number): number {
  const exp = Math.pow(10, Math.floor(Math.log10(value)));
  const f = value / exp;
  return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * exp;
}
