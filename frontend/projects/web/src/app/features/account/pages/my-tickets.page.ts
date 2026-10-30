import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTabsModule } from 'ng-zorro-antd/tabs';

import { TicketCardComponent } from '../../../shared/components/ticket-card.component';
import { MyBookingStore } from '../my-booking.store';

/** Vé của tôi: vé sắp chiếu (kèm QR) và vé đã xem (BKG10). */
@Component({
  selector: 'app-my-tickets-page',
  imports: [RouterLink, NzCardModule, NzTabsModule, NzEmptyModule, NzSkeletonModule, NzButtonModule, TicketCardComponent],
  template: `
    <nz-card nzTitle="Vé của tôi" [nzExtra]="ordersLink">
      <nz-tabset (nzSelectedIndexChange)="store.loadTickets($event === 1)">
        <nz-tab nzTitle="Sắp chiếu"></nz-tab>
        <nz-tab nzTitle="Đã xem"></nz-tab>
      </nz-tabset>
      @if (store.tickets(); as tickets) {
        @if (tickets.length === 0) {
          <nz-empty nzNotFoundContent="Chưa có vé nào" [nzNotFoundFooter]="footer" />
          <ng-template #footer><a nz-button nzType="primary" routerLink="/movies">Đặt vé ngay</a></ng-template>
        } @else {
          <div class="tickets">
            @for (t of tickets; track t.code) {
              <app-ticket-card [code]="t.code" [seatLabel]="t.seatLabel" [status]="t.status" [qrImage]="t.qrImage" [showtime]="t.showtime" />
            }
          </div>
        }
      } @else {
        <nz-skeleton [nzActive]="true" />
      }
    </nz-card>
    <ng-template #ordersLink><a routerLink="/account/orders">Lịch sử đơn hàng</a></ng-template>
  `,
  styles: `
    nz-card {
      max-width: 860px;
      margin: 0 auto;
    }

    .tickets {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
      gap: 12px;
    }

    @media (max-width: 576px) {
      .tickets {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class MyTicketsPage implements OnInit {
  protected readonly store = inject(MyBookingStore);

  ngOnInit(): void {
    this.store.loadTickets(false);
  }
}
