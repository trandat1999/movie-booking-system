import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AUDIO_LABELS, FORMAT_LABELS, VN_OFFSET } from '@mb/shared';

import { ShowtimeVersion } from '../showtime-groups';

/** Các nút giờ chiếu theo từng phiên bản (2D · Phụ đề, IMAX · Lồng tiếng…). */
@Component({
  selector: 'app-showtime-times',
  imports: [DatePipe, RouterLink],
  template: `
    @for (version of versions(); track version.format + version.audioType) {
      <div class="version">
        <div class="version-label">{{ formatLabels[version.format] }} · {{ audioLabels[version.audioType] }}</div>
        <div class="times">
          @for (s of version.showtimes; track s.id) {
            @if (s.availableSeats > 0) {
              <a class="time" [routerLink]="['/showtimes', s.id]" [attr.aria-label]="'Suất ' + (s.startAt | date: 'HH:mm' : vn)">
                <strong>{{ s.startAt | date: 'HH:mm' : vn }}</strong>
                <small>{{ s.availableSeats }} ghế trống</small>
              </a>
            } @else {
              <span class="time full">
                <strong>{{ s.startAt | date: 'HH:mm' : vn }}</strong>
                <small>Hết ghế</small>
              </span>
            }
          }
        </div>
      </div>
    }
  `,
  styles: `
    .version + .version {
      margin-top: 12px;
    }

    .version-label {
      margin-bottom: 6px;
      color: rgba(0, 0, 0, 0.65);
      font-weight: 500;
    }

    .times {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .time {
      display: flex;
      flex-direction: column;
      align-items: center;
      min-width: 84px;
      padding: 6px 10px;
      border: 1px solid #d9d9d9;
      border-radius: 6px;
      color: inherit;
    }

    a.time:hover {
      border-color: #1677ff;
      color: #1677ff;
    }

    .time small {
      color: rgba(0, 0, 0, 0.45);
      font-size: 11px;
    }

    .time.full {
      background: #f5f5f5;
      color: rgba(0, 0, 0, 0.25);
      cursor: not-allowed;
    }
  `,
})
export class ShowtimeTimesComponent {
  readonly versions = input.required<ShowtimeVersion[]>();

  protected readonly vn = VN_OFFSET;
  protected readonly formatLabels = FORMAT_LABELS;
  protected readonly audioLabels = AUDIO_LABELS;
}
