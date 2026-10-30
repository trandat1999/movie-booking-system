import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  ACTIVE_STATUS_LABELS,
  ActiveStatus,
  CinemaRequest,
  Room,
  ROOM_TYPE_LABELS,
  RoomType,
  toOptions,
} from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDescriptionsModule } from 'ng-zorro-antd/descriptions';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CinemaFormModalComponent } from '../components/cinema-form-modal.component';
import { CinemasStore } from '../cinemas.store';

/** Chi tiết rạp và danh sách phòng chiếu (CIN03). */
@Component({
  selector: 'adm-cinema-detail-page',
  imports: [
    FormsModule,
    RouterLink,
    NzCardModule,
    NzDescriptionsModule,
    NzTableModule,
    NzButtonModule,
    NzTagModule,
    NzModalModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
    CinemaFormModalComponent,
  ],
  templateUrl: './cinema-detail.page.html',
  styles: `
    nz-card + nz-card {
      margin-top: 16px;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }

    .actions > * + * {
      margin-left: 8px;
    }
  `,
})
export class CinemaDetailPage implements OnInit {
  protected readonly store = inject(CinemasStore);
  private readonly message = inject(NzMessageService);

  readonly cinemaId = input.required<string>();

  protected readonly statusLabels = ACTIVE_STATUS_LABELS;
  protected readonly roomTypeLabels = ROOM_TYPE_LABELS;
  protected readonly roomTypeOptions = toOptions(ROOM_TYPE_LABELS);
  protected readonly statusOptions = toOptions(ACTIVE_STATUS_LABELS);

  protected readonly editingCinema = signal<boolean>(false);
  /** undefined = đóng, null = thêm phòng */
  protected readonly editingRoom = signal<Room | null | undefined>(undefined);
  protected roomName = '';
  protected roomType: RoomType = 'STANDARD';
  protected roomStatus: ActiveStatus = 'ACTIVE';

  ngOnInit(): void {
    this.store.loadCinema(Number(this.cinemaId()));
  }

  protected saveCinema(request: CinemaRequest): void {
    this.store.saveCinema(Number(this.cinemaId()), request).subscribe(() => {
      this.message.success('Đã lưu rạp');
      this.editingCinema.set(false);
    });
  }

  protected openRoom(room: Room | null): void {
    this.roomName = room?.name ?? '';
    this.roomType = room?.roomType ?? 'STANDARD';
    this.roomStatus = room?.status ?? 'ACTIVE';
    this.editingRoom.set(room);
  }

  protected saveRoom(): void {
    if (!this.roomName.trim()) {
      return;
    }
    this.store
      .saveRoom(Number(this.cinemaId()), this.editingRoom()?.id ?? null, {
        name: this.roomName.trim(),
        roomType: this.roomType,
        status: this.roomStatus,
      })
      .subscribe(() => {
        this.message.success('Đã lưu phòng chiếu');
        this.editingRoom.set(undefined);
      });
  }
}
