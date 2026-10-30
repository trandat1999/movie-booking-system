import { Component, input, output } from '@angular/core';
import { DayTab } from '@mb/shared';

/** Thanh chọn ngày chiếu (7 ngày tới). */
@Component({
  selector: 'app-day-tabs',
  template: `
    <div class="tabs" role="tablist" aria-label="Chọn ngày chiếu">
      @for (day of days(); track day.date) {
        <button
          type="button"
          role="tab"
          class="tab"
          [class.active]="day.date === selected()"
          [attr.aria-selected]="day.date === selected()"
          (click)="selectedChange.emit(day.date)"
        >
          <span class="label">{{ day.label }}</span>
          <span class="date">{{ day.dayMonth }}</span>
        </button>
      }
    </div>
  `,
  styles: `
    .tabs {
      display: flex;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 4px;
    }

    .tab {
      display: flex;
      flex: 0 0 auto;
      flex-direction: column;
      align-items: center;
      min-width: 76px;
      padding: 6px 12px;
      border: 1px solid #d9d9d9;
      border-radius: 8px;
      background: #fff;
      cursor: pointer;
    }

    .tab:hover {
      border-color: #1677ff;
    }

    .tab.active {
      border-color: #1677ff;
      background: #1677ff;
      color: #fff;
    }

    .label {
      font-size: 12px;
    }

    .date {
      font-weight: 600;
    }
  `,
})
export class DayTabsComponent {
  readonly days = input.required<DayTab[]>();
  readonly selected = input.required<string>();
  readonly selectedChange = output<string>();
}
