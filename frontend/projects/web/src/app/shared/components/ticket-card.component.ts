import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { BookingShowtimeInfo, FORMAT_LABELS, TICKET_STATUS_LABELS, TicketStatus, VN_OFFSET } from '@mb/shared';
import { NzTagModule } from 'ng-zorro-antd/tag';

/** Vé điện tử: QR + thông tin suất chiếu (BKG08, BKG10). */
@Component({
  selector: 'app-ticket-card',
  imports: [DatePipe, NzTagModule],
  template: `
    <div class="ticket" [class.inactive]="status() !== 'VALID'">
      <div class="qr">
        @if (qrImage(); as qr) {
          <img [src]="qr" [alt]="'Mã QR vé ' + code()" width="140" height="140" />
        } @else {
          <div class="no-qr">{{ statusLabels[status()] }}</div>
        }
      </div>
      <div class="body">
        <div class="seat">Ghế {{ seatLabel() }}</div>
        @if (showtime(); as s) {
          <div class="movie">{{ s.movieTitle }}</div>
          <div>{{ s.startAt | date: 'HH:mm · dd/MM/yyyy' : vn }} · {{ formatLabels[s.format] }}</div>
          <div class="muted">{{ s.cinemaName }} · {{ s.roomName }}</div>
        }
        <div class="code">
          Mã vé: <code>{{ code() }}</code>
          @if (status() !== 'VALID') {
            <nz-tag>{{ statusLabels[status()] }}</nz-tag>
          }
        </div>
      </div>
    </div>
  `,
  styles: `
    .ticket {
      display: flex;
      gap: 16px;
      padding: 12px;
      border: 1px dashed #d9d9d9;
      border-radius: 8px;
      background: #fff;
    }

    .inactive {
      opacity: 0.6;
    }

    .qr img,
    .no-qr {
      display: block;
      width: 140px;
      height: 140px;
    }

    .no-qr {
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f5f5;
      color: #8c8c8c;
    }

    .body {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }

    .seat {
      font-size: 20px;
      font-weight: 600;
    }

    .movie {
      font-weight: 500;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .code {
      margin-top: auto;
    }

    @media (max-width: 420px) {
      .ticket {
        flex-direction: column;
        align-items: center;
        text-align: center;
      }
    }
  `,
})
export class TicketCardComponent {
  readonly code = input.required<string>();
  readonly seatLabel = input.required<string>();
  readonly status = input.required<TicketStatus>();
  readonly qrImage = input<string | null>(null);
  readonly showtime = input<BookingShowtimeInfo | null>(null);

  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = TICKET_STATUS_LABELS;
  protected readonly formatLabels = FORMAT_LABELS;
}
