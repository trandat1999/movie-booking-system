import { DatePipe } from '@angular/common';
import { Component, effect, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  DAY_TYPE_LABELS,
  DayType,
  FORMAT_LABELS,
  ScreenFormat,
  SeatType,
  toIsoDate,
  VndPipe,
} from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzTableModule } from 'ng-zorro-antd/table';

import { PricingStore } from './pricing.store';

const FORMATS: ScreenFormat[] = ['TWO_D', 'THREE_D', 'IMAX'];
const DAY_TYPES: DayType[] = ['WEEKDAY', 'WEEKEND', 'HOLIDAY'];

/** Giá vé = giá cơ bản(định dạng, loại ngày) × số chỗ của ghế + phụ thu loại ghế. */
@Component({
  selector: 'adm-pricing-page',
  imports: [
    DatePipe,
    FormsModule,
    VndPipe,
    NzCardModule,
    NzTableModule,
    NzInputNumberModule,
    NzInputModule,
    NzButtonModule,
    NzDatePickerModule,
    NzPopconfirmModule,
  ],
  templateUrl: './pricing.page.html',
  styleUrl: './pricing.page.less',
})
export class PricingPage implements OnInit {
  protected readonly store = inject(PricingStore);
  private readonly message = inject(NzMessageService);

  protected readonly formats = FORMATS;
  protected readonly dayTypes = DAY_TYPES;
  protected readonly formatLabels = FORMAT_LABELS;
  protected readonly dayTypeLabels = DAY_TYPE_LABELS;

  /** Bản nháp ma trận giá: "TWO_D:WEEKDAY" → giá (null khi chưa có — nz-input-number không nhận undefined) */
  protected matrix: Record<string, number | null> = {};
  protected holidayDate: Date | null = null;
  protected holidayName = '';

  constructor() {
    effect(() => {
      const prices = new Map(this.store.basePrices().map((p) => [`${p.format}:${p.dayType}`, p.price]));
      this.matrix = Object.fromEntries(
        FORMATS.flatMap((f) => DAY_TYPES.map((d) => [`${f}:${d}`, prices.get(`${f}:${d}`) ?? null])),
      );
    });
  }

  ngOnInit(): void {
    this.store.load();
  }

  protected example(format: ScreenFormat, dayType: DayType, type: SeatType): number {
    return (this.matrix[`${format}:${dayType}`] ?? 0) * type.capacity + type.surcharge;
  }

  protected saveMatrix(): void {
    const items = FORMATS.flatMap((format) =>
      DAY_TYPES.map((dayType) => ({ format, dayType, price: this.matrix[`${format}:${dayType}`] ?? 0 })),
    );
    this.store.saveBasePrices(items).subscribe(() => this.message.success('Đã lưu bảng giá — áp dụng cho suất chiếu tạo mới'));
  }

  protected saveSeatType(type: SeatType): void {
    this.store.saveSeatType(type).subscribe(() => this.message.success(`Đã lưu ${type.name}`));
  }

  protected addHoliday(): void {
    const date = toIsoDate(this.holidayDate);
    if (!date || !this.holidayName.trim()) {
      return;
    }
    this.store.addHoliday({ date, name: this.holidayName.trim() }).subscribe(() => {
      this.message.success('Đã thêm ngày lễ');
      this.holidayDate = null;
      this.holidayName = '';
    });
  }

  protected deleteHoliday(date: string): void {
    this.store.deleteHoliday(date).subscribe(() => this.message.success('Đã xóa ngày lễ'));
  }
}
