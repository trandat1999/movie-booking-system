import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AuthStore,
  CatalogApi,
  GroupApi,
  GroupChangedMessage,
  GroupDetail,
  GroupSummary,
  PublicShowtime,
  RealtimeService,
  RsvpRequest,
  SeatMap,
  SeatPreferenceRequest,
} from '@mb/shared';
import { finalize, map, Observable, Subscription, tap } from 'rxjs';

/**
 * Trạng thái một nhóm cho trang chi tiết (GRP03–GRP09, GRP15–GRP19). Mọi thay đổi đi qua REST; sự kiện
 * `/topic/groups/{id}` chỉ báo "có thay đổi" → tải lại snapshot (PLAN.md 8.3).
 */
@Injectable()
export class GroupStore {
  private readonly api = inject(GroupApi);
  private readonly catalogApi = inject(CatalogApi);
  private readonly authStore = inject(AuthStore);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly detail = signal<GroupDetail | null>(null);
  readonly summary = signal<GroupSummary | null>(null);
  readonly notFound = signal(false);
  readonly busy = signal(false);
  /** Sơ đồ ghế suất đã chốt (để thành viên xem phương án trưởng nhóm đang chọn / đã giữ) */
  readonly lockedSeatMap = signal<SeatMap | null>(null);
  /** Ghế trưởng nhóm đang xem trước (GRP15) */
  readonly previewSeatIds = signal<number[]>([]);

  readonly isLeader = computed(() => this.detail()?.myRole === 'LEADER');
  readonly myUserId = computed(() => this.authStore.user()?.id ?? null);
  readonly me = computed(() => this.detail()?.members.find((m) => m.userId === this.myUserId()) ?? null);

  /** Suất chiếu của phim nhóm trong ngày đang chọn ở hộp thoại đề xuất suất */
  readonly showtimes = signal<PublicShowtime[] | null>(null);

  private groupId = 0;
  private sub?: Subscription;

  /** @param date yyyy-MM-dd giờ Việt Nam */
  loadShowtimes(date: string): void {
    const group = this.detail();
    if (!group) {
      return;
    }
    this.showtimes.set(null);
    this.catalogApi
      .searchShowtimes({ movieId: group.movie.id, cinemaId: group.cinema?.id ?? null, date })
      .subscribe((list) => this.showtimes.set(list));
  }

  load(groupId: number): void {
    this.groupId = groupId;
    this.detail.set(null);
    this.summary.set(null);
    this.notFound.set(false);
    this.previewSeatIds.set([]);
    this.refresh();
    this.sub?.unsubscribe();
    this.sub = this.realtime
      .watch<GroupChangedMessage>(`/topic/groups/${groupId}`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((message) => {
        if (message.type === 'SEATS_PREVIEWED') {
          this.previewSeatIds.set(message.seatIds ?? []);
          return;
        }
        if (message.type === 'SEATS_HELD' || message.type === 'STATUS_CHANGED') {
          this.previewSeatIds.set([]);
        }
        this.refresh();
      });
  }

  refresh(): void {
    this.api.get(this.groupId).subscribe({
      next: (detail) => this.setDetail(detail),
      error: () => this.notFound.set(true),
    });
    this.api.summary(this.groupId).subscribe({ next: (s) => this.summary.set(s), error: () => undefined });
  }

  seatPreference(request: SeatPreferenceRequest): Observable<GroupDetail> {
    return this.apply(this.api.seatPreference(this.groupId, request));
  }

  rsvp(request: RsvpRequest): Observable<GroupDetail> {
    return this.apply(this.api.rsvp(this.groupId, request));
  }

  propose(showtimeId: number): Observable<GroupDetail> {
    return this.apply(this.api.propose(this.groupId, showtimeId));
  }

  removeOption(optionId: number): Observable<GroupDetail> {
    return this.apply(this.api.removeOption(this.groupId, optionId));
  }

  vote(optionId: number, voted: boolean): Observable<GroupDetail> {
    return this.apply(this.api.vote(this.groupId, optionId, voted));
  }

  lock(showtimeId: number, ticketCount: number | null): Observable<GroupDetail> {
    return this.apply(this.api.lock(this.groupId, showtimeId, ticketCount));
  }

  unlock(): Observable<GroupDetail> {
    return this.apply(this.api.unlock(this.groupId));
  }

  cancel(): Observable<GroupDetail> {
    return this.apply(this.api.cancel(this.groupId));
  }

  releaseHold(): Observable<GroupDetail> {
    return this.apply(this.api.releaseHold(this.groupId));
  }

  removeMember(userId: number): Observable<GroupDetail> {
    return this.apply(this.api.removeMember(this.groupId, userId));
  }

  transferLeader(userId: number): Observable<GroupDetail> {
    return this.apply(this.api.transferLeader(this.groupId, userId));
  }

  regenerateCode(): Observable<GroupDetail> {
    return this.apply(this.api.regenerateCode(this.groupId));
  }

  updateAssignments(items: { ticketCode: string; userId: number; guestLabel: string | null }[]): Observable<GroupDetail> {
    return this.apply(this.api.updateAssignments(this.groupId, items));
  }

  leave(): Observable<void> {
    this.busy.set(true);
    return this.api.leave(this.groupId).pipe(finalize(() => this.busy.set(false)));
  }

  /** Mã đơn để chuyển sang trang thanh toán. */
  createOrder(): Observable<string> {
    this.busy.set(true);
    return this.api.createOrder(this.groupId).pipe(
      map((order) => order.code),
      finalize(() => this.busy.set(false)),
    );
  }

  private apply(request: Observable<GroupDetail>): Observable<GroupDetail> {
    this.busy.set(true);
    return request.pipe(
      tap((detail) => {
        this.setDetail(detail);
        this.api.summary(this.groupId).subscribe({ next: (s) => this.summary.set(s), error: () => undefined });
      }),
      finalize(() => this.busy.set(false)),
    );
  }

  private setDetail(detail: GroupDetail): void {
    const previousShowtime = this.detail()?.locked?.showtime.id;
    this.detail.set(detail);
    const showtimeId = detail.locked?.showtime.id;
    if (!showtimeId) {
      this.lockedSeatMap.set(null);
    } else if (showtimeId !== previousShowtime || detail.status !== 'BOOKED') {
      this.catalogApi.getSeatMap(showtimeId).subscribe({
        next: (map) => this.lockedSeatMap.set(map),
        error: () => this.lockedSeatMap.set(null),
      });
    }
  }
}
