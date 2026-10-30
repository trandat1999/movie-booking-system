import { DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  AGE_RATING_COLORS,
  AUDIO_LABELS,
  DayTab,
  FORMAT_LABELS,
  GROUP_STATUS_COLORS,
  GROUP_STATUS_LABELS,
  GridSeat,
  GridSeatState,
  GroupMemberView,
  MoviePosterComponent,
  nextDays,
  RSVP_COLORS,
  RSVP_LABELS,
  RsvpRequest,
  SeatGridComponent,
  SeatStatus,
  VN_OFFSET,
  VndPipe,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CountdownComponent } from '../../../shared/components/countdown.component';
import { DayTabsComponent } from '../../../shared/components/day-tabs.component';
import { RsvpFormComponent } from '../components/rsvp-form.component';
import { GroupStore } from '../group.store';

const CLUSTER_LABELS: Record<string, { text: string; color: string }> = {
  SAME_ROW: { text: 'Còn chỗ ngồi liền nhau', color: 'green' },
  SPLIT: { text: 'Phải chia hàng', color: 'gold' },
  NONE: { text: 'Không đủ ghế gần nhau', color: 'red' },
  UNAVAILABLE: { text: 'Suất không còn bán', color: 'default' },
};

const SEAT_STATE: Record<SeatStatus, GridSeatState> = {
  AVAILABLE: 'normal',
  HELD: 'held',
  SOLD: 'sold',
  BLOCKED: 'inactive',
};

/** Trang tiến trình nhóm (GRP03–GRP09, GRP15–GRP19). */
@Component({
  selector: 'app-group-detail-page',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    VndPipe,
    NzAlertModule,
    NzButtonModule,
    NzCardModule,
    NzDropDownModule,
    NzEmptyModule,
    NzInputNumberModule,
    NzModalModule,
    NzProgressModule,
    NzResultModule,
    NzSelectModule,
    NzSkeletonModule,
    NzTableModule,
    NzTagModule,
    MoviePosterComponent,
    SeatGridComponent,
    CountdownComponent,
    DayTabsComponent,
    RsvpFormComponent,
  ],
  templateUrl: './group-detail.page.html',
  styleUrl: './group-detail.page.less',
})
export class GroupDetailPage {
  protected readonly store = inject(GroupStore);
  private readonly router = inject(Router);
  private readonly message = inject(NzMessageService);
  private readonly modal = inject(NzModalService);

  readonly id = input.required<string>();

  protected readonly vn = VN_OFFSET;
  protected readonly statusLabels = GROUP_STATUS_LABELS;
  protected readonly statusColors = GROUP_STATUS_COLORS;
  protected readonly rsvpLabels = RSVP_LABELS;
  protected readonly rsvpColors = RSVP_COLORS;
  protected readonly ratingColors = AGE_RATING_COLORS;
  protected readonly formatLabels = FORMAT_LABELS;
  protected readonly audioLabels = AUDIO_LABELS;
  protected readonly clusterLabels = CLUSTER_LABELS;

  // Hộp thoại đề xuất suất
  protected readonly proposeOpen = signal(false);
  protected readonly days = signal<DayTab[]>([]);
  protected readonly selectedDay = signal('');

  // Hộp thoại chốt suất
  protected readonly lockOptionId = signal<number | null>(null);
  protected lockTickets = 1;

  // Phân phối vé (trưởng nhóm sửa)
  protected readonly editingAssignments = signal(false);
  protected assignmentDraft: Record<string, number> = {};

  protected readonly inviteLink = computed(() => {
    const code = this.store.detail()?.code;
    return code ? `${location.origin}/groups/join/${code}` : '';
  });

  protected readonly pendingNames = computed(() =>
    (this.store.detail()?.members ?? [])
      .filter((m) => m.rsvp === 'PENDING')
      .map((m) => m.fullName)
      .join(', '),
  );

  protected readonly respondedPercent = computed(() => {
    const p = this.store.detail()?.progress;
    return p && p.members > 0 ? Math.round((p.responded / p.members) * 100) : 0;
  });

  protected readonly optionsWithSummary = computed(() => {
    const detail = this.store.detail();
    const summary = new Map((this.store.summary()?.options ?? []).map((o) => [o.optionId, o]));
    return (detail?.options ?? [])
      .map((o) => ({ ...o, summary: summary.get(o.id) ?? null }))
      .sort((a, b) => b.votes - a.votes || a.showtime.startAt.localeCompare(b.showtime.startAt));
  });

  /** Ghế được tô trên sơ đồ: đã giữ (HOLDING), đang xem trước (LOCKED), đã đặt (BOOKED). */
  protected readonly highlighted = computed(() => {
    const detail = this.store.detail();
    if (detail?.status === 'HOLDING' && detail.hold) {
      return new Set(detail.hold.seatIds);
    }
    if (detail?.status === 'BOOKED') {
      const labels = new Set(detail.assignments.map((a) => a.seatLabel));
      return new Set(
        (this.store.lockedSeatMap()?.seats ?? []).filter((s) => labels.has(s.rowLabel + s.number)).map((s) => s.id),
      );
    }
    return new Set(this.store.previewSeatIds());
  });

  protected readonly gridSeats = computed<GridSeat[]>(() => {
    const highlighted = this.highlighted();
    return (this.store.lockedSeatMap()?.seats ?? []).map((s) => ({
      gridRow: s.gridRow,
      gridCol: s.gridCol,
      colSpan: s.colSpan,
      seatTypeCode: s.seatTypeCode,
      label: s.rowLabel + s.number,
      state: highlighted.has(s.id) ? 'selected' : SEAT_STATE[s.status],
      ref: s,
    }));
  });

  protected readonly seatColors = computed(() =>
    Object.fromEntries((this.store.lockedSeatMap()?.seatTypes ?? []).map((t) => [t.code, t.color])),
  );

  constructor() {
    effect(() => this.store.load(Number(this.id())));
  }

  protected copyInvite(): void {
    void navigator.clipboard?.writeText(this.inviteLink()).then(
      () => this.message.success('Đã sao chép link mời'),
      () => this.message.info(this.inviteLink()),
    );
  }

  protected saveRsvp(request: RsvpRequest): void {
    this.store.rsvp(request).subscribe(() => this.message.success('Đã lưu phản hồi'));
  }

  protected openPropose(): void {
    const detail = this.store.detail()!;
    const days = nextDays(14).filter(
      (d) => (!detail.dateFrom || d.date >= detail.dateFrom) && (!detail.dateTo || d.date <= detail.dateTo),
    );
    this.days.set(days);
    this.proposeOpen.set(true);
    if (days.length > 0) {
      this.selectDay(days[0].date);
    }
  }

  protected selectDay(date: string): void {
    this.selectedDay.set(date);
    this.store.loadShowtimes(date);
  }

  protected propose(showtimeId: number): void {
    this.store.propose(showtimeId).subscribe(() => {
      this.proposeOpen.set(false);
      this.message.success('Đã đề xuất suất chiếu');
    });
  }

  protected isProposed(showtimeId: number): boolean {
    return (this.store.detail()?.options ?? []).some((o) => o.showtime.id === showtimeId);
  }

  protected toggleVote(optionId: number, voted: boolean): void {
    this.store.vote(optionId, !voted).subscribe();
  }

  protected removeOption(optionId: number): void {
    this.store.removeOption(optionId).subscribe();
  }

  protected openLock(optionId: number): void {
    this.lockTickets = Math.max(1, this.store.summary()?.ticketCount ?? 1);
    this.lockOptionId.set(optionId);
  }

  protected confirmLock(): void {
    const option = this.store.detail()?.options.find((o) => o.id === this.lockOptionId());
    if (!option) {
      return;
    }
    this.store.lock(option.showtime.id, this.lockTickets).subscribe(() => {
      this.lockOptionId.set(null);
      this.message.success('Đã chốt suất chiếu — giờ hãy chọn ghế cho nhóm');
    });
  }

  protected unlock(): void {
    this.modal.confirm({
      nzTitle: 'Mở chốt để đổi suất?',
      nzContent: 'Nhóm quay lại bước bình chọn; thành viên sẽ nhận thông báo.',
      nzOnOk: () => this.store.unlock().subscribe(),
    });
  }

  protected pay(): void {
    this.store.createOrder().subscribe((code) => void this.router.navigate(['/checkout', code]));
  }

  protected releaseHold(): void {
    this.store.releaseHold().subscribe(() => void this.router.navigate(['/groups', this.id(), 'seats']));
  }

  protected cancelGroup(): void {
    this.modal.confirm({
      nzTitle: 'Hủy nhóm?',
      nzContent: 'Mọi thành viên sẽ nhận thông báo. Ghế đang giữ (nếu có) được trả lại.',
      nzOkText: 'Hủy nhóm',
      nzOkDanger: true,
      nzCancelText: 'Không',
      nzOnOk: () => this.store.cancel().subscribe(),
    });
  }

  protected leave(): void {
    this.modal.confirm({
      nzTitle: 'Rời nhóm?',
      nzOnOk: () =>
        this.store.leave().subscribe(() => {
          this.message.success('Bạn đã rời nhóm');
          void this.router.navigateByUrl('/groups');
        }),
    });
  }

  protected removeMember(member: GroupMemberView): void {
    this.modal.confirm({
      nzTitle: `Mời ${member.fullName} ra khỏi nhóm?`,
      nzOkDanger: true,
      nzOnOk: () => this.store.removeMember(member.userId).subscribe(),
    });
  }

  protected makeLeader(member: GroupMemberView): void {
    this.modal.confirm({
      nzTitle: `Chuyển quyền trưởng nhóm cho ${member.fullName}?`,
      nzOnOk: () => this.store.transferLeader(member.userId).subscribe(),
    });
  }

  protected regenerateCode(): void {
    this.store.regenerateCode().subscribe(() => this.message.success('Đã tạo mã mời mới, mã cũ hết hiệu lực'));
  }

  protected editAssignments(): void {
    this.assignmentDraft = Object.fromEntries(
      (this.store.detail()?.assignments ?? []).map((a) => [a.ticketCode, a.userId]),
    );
    this.editingAssignments.set(true);
  }

  protected saveAssignments(): void {
    const current = new Map((this.store.detail()?.assignments ?? []).map((a) => [a.ticketCode, a]));
    const items = Object.entries(this.assignmentDraft).map(([ticketCode, userId]) => {
      const previous = current.get(ticketCode);
      // Đổi người giữ vé thì bỏ nhãn "khách của ..." cũ
      return { ticketCode, userId, guestLabel: previous?.userId === userId ? previous.guestLabel : null };
    });
    this.store.updateAssignments(items).subscribe(() => {
      this.editingAssignments.set(false);
      this.message.success('Đã cập nhật phân phối vé');
    });
  }
}
