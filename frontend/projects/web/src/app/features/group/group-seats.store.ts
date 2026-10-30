import { HttpErrorResponse } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ApiResponse,
  CatalogApi,
  GroupApi,
  GroupDetail,
  RealtimeService,
  resolveErrorMessage,
  SeatMap,
  SeatSuggestions,
  SeatSuggestionView,
  SeatsChangedMessage,
  SuggestionOptions,
} from '@mb/shared';
import { finalize, map, Observable, Subscription, switchMap, tap } from 'rxjs';

/**
 * Chọn ghế cho nhóm (GRP10–GRP14): phương án đề xuất, chọn tay + chấm điểm, xem trước cho thành viên, giữ ghế
 * nguyên khối và tạo đơn.
 */
@Injectable()
export class GroupSeatsStore {
  private readonly api = inject(GroupApi);
  private readonly catalogApi = inject(CatalogApi);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly group = signal<GroupDetail | null>(null);
  readonly seatMap = signal<SeatMap | null>(null);
  readonly notFound = signal(false);
  readonly suggestions = signal<SeatSuggestions | null>(null);
  readonly loadingSuggestions = signal(false);
  readonly busy = signal(false);
  /** Ghế đang chọn (từ phương án hoặc chọn tay) */
  readonly selectedIds = signal<number[]>([]);
  /** Thứ hạng phương án đang chọn; 0 = đã chỉnh tay */
  readonly selectedRank = signal<number | null>(null);
  /** Điểm của lựa chọn hiện tại khi chọn tay */
  readonly evaluation = signal<SeatSuggestionView | null>(null);
  readonly evaluationError = signal<string | null>(null);
  /** Thông báo khi giữ ghế thất bại vì ghế vừa bị người khác giữ */
  readonly conflict = signal<string | null>(null);

  readonly ticketCount = computed(() => this.group()?.locked?.ticketCount ?? 0);
  readonly selectedCapacity = computed(() => {
    const capacities = new Map((this.seatMap()?.seatTypes ?? []).map((t) => [t.code, t.capacity]));
    const ids = new Set(this.selectedIds());
    return (this.seatMap()?.seats ?? [])
      .filter((s) => ids.has(s.id))
      .reduce((sum, s) => sum + (capacities.get(s.seatTypeCode) ?? 1), 0);
  });
  /** Đủ chỗ cho nhóm (ghế đôi có thể dư 1 chỗ) */
  readonly selectionValid = computed(
    () => this.selectedCapacity() >= this.ticketCount() && this.selectedCapacity() <= this.ticketCount() + 1,
  );

  private groupId = 0;
  private seatsSub?: Subscription;

  load(groupId: number): void {
    this.groupId = groupId;
    this.notFound.set(false);
    this.api.get(groupId).subscribe({
      next: (group) => {
        this.group.set(group);
        if (group.locked) {
          this.loadSeatMap(group.locked.showtime.id);
        }
      },
      error: () => this.notFound.set(true),
    });
  }

  suggest(options: SuggestionOptions): void {
    this.loadingSuggestions.set(true);
    this.api
      .suggestSeats(this.groupId, options)
      .pipe(finalize(() => this.loadingSuggestions.set(false)))
      .subscribe((result) => {
        this.suggestions.set(result);
        if (result.options.length > 0) {
          this.choose(result.options[0]);
        }
      });
  }

  choose(option: SeatSuggestionView): void {
    this.selectedIds.set(option.seatIds);
    this.selectedRank.set(option.rank);
    this.evaluation.set(option);
    this.evaluationError.set(null);
    this.conflict.set(null);
    this.api.previewSeats(this.groupId, option.seatIds).subscribe({ error: () => undefined });
  }

  /** Chọn/bỏ một ghế bằng tay rồi chấm điểm lại (GRP13). */
  toggleSeat(seatId: number): void {
    const ids = this.selectedIds();
    this.selectedIds.set(ids.includes(seatId) ? ids.filter((id) => id !== seatId) : [...ids, seatId]);
    this.selectedRank.set(0);
    this.conflict.set(null);
    this.evaluate();
  }

  clearSelection(): void {
    this.selectedIds.set([]);
    this.selectedRank.set(null);
    this.evaluation.set(null);
    this.evaluationError.set(null);
  }

  /** Giữ ghế → tạo đơn; trả mã đơn để sang trang thanh toán. Ghế bị mất → hiện phương án mới. */
  holdAndOrder(): Observable<string> {
    this.busy.set(true);
    this.conflict.set(null);
    const logId = this.suggestions()?.logId ?? null;
    return this.api.hold(this.groupId, this.selectedIds(), logId, this.selectedRank()).pipe(
      tap((group) => this.group.set(group)),
      switchMap(() => this.api.createOrder(this.groupId)),
      map((order) => order.code),
      tap({ error: (error: unknown) => this.handleHoldError(error) }),
      finalize(() => this.busy.set(false)),
    );
  }

  private handleHoldError(error: unknown): void {
    if (!(error instanceof HttpErrorResponse)) {
      return;
    }
    const body = error.error as ApiResponse<unknown> | null;
    const details = body?.error?.details as { suggestions?: SeatSuggestions } | undefined;
    this.conflict.set(resolveErrorMessage(error));
    if (body?.error?.code === 'SEAT_UNAVAILABLE' && details?.suggestions) {
      this.suggestions.set({ ...details.suggestions, logId: this.suggestions()?.logId ?? null });
      this.clearSelection();
      if (this.group()?.locked) {
        this.loadSeatMap(this.group()!.locked!.showtime.id);
      }
    }
  }

  private evaluate(): void {
    if (!this.selectionValid()) {
      this.evaluation.set(null);
      this.evaluationError.set(
        `Đã chọn ${this.selectedCapacity()} chỗ — cần ${this.ticketCount()} chỗ cho nhóm.`,
      );
      return;
    }
    this.evaluationError.set(null);
    const ids = this.selectedIds();
    this.api.evaluateSeats(this.groupId, ids).subscribe({
      next: (view) => {
        this.evaluation.set(view);
        this.api.previewSeats(this.groupId, ids).subscribe({ error: () => undefined });
      },
      error: (e: HttpErrorResponse) => this.evaluationError.set(resolveErrorMessage(e)),
    });
  }

  private loadSeatMap(showtimeId: number): void {
    this.catalogApi.getSeatMap(showtimeId).subscribe((map) => this.seatMap.set(map));
    this.seatsSub?.unsubscribe();
    this.seatsSub = this.realtime
      .watch<SeatsChangedMessage>(`/topic/showtimes/${showtimeId}/seats`)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((message) => {
        const changes = new Map(message.seats.map((s) => [s.id, s.status]));
        this.seatMap.update((m) =>
          m ? { ...m, seats: m.seats.map((s) => (changes.has(s.id) ? { ...s, status: changes.get(s.id)! } : s)) } : m,
        );
      });
  }
}
