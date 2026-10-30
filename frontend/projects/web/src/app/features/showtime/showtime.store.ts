import { HttpErrorResponse } from '@angular/common/http';
import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ApiResponse,
  BookingApi,
  CatalogApi,
  Order,
  PublicShowtimeDetail,
  RealtimeService,
  resolveErrorMessage,
  SeatMap,
  SeatMapSeat,
  SeatsChangedMessage,
} from '@mb/shared';
import { finalize, forkJoin, map, Observable, Subscription, switchMap, tap } from 'rxjs';

export interface BookingError {
  code: string | null;
  message: string;
}

/**
 * Thông tin suất chiếu, sơ đồ ghế cập nhật realtime và ghế đang chọn (SCH05–SCH07, BKG01–BKG03, BKG05).
 */
@Injectable()
export class ShowtimeStore {
  private readonly catalogApi = inject(CatalogApi);
  private readonly bookingApi = inject(BookingApi);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly detail = signal<PublicShowtimeDetail | null>(null);
  readonly seatMap = signal<SeatMap | null>(null);
  readonly notFound = signal(false);
  readonly selectedIds = signal<ReadonlySet<number>>(new Set());
  readonly submitting = signal(false);

  /** Ghế đang chọn, theo thứ tự hàng/cột */
  readonly selectedSeats = computed<SeatMapSeat[]>(() => {
    const ids = this.selectedIds();
    return (this.seatMap()?.seats ?? [])
      .filter((s) => ids.has(s.id))
      .sort((a, b) => a.gridRow - b.gridRow || a.gridCol - b.gridCol);
  });

  readonly prices = computed(() => new Map((this.seatMap()?.seatTypes ?? []).map((t) => [t.code, t.price])));

  readonly total = computed(() =>
    this.selectedSeats().reduce((sum, seat) => sum + (this.prices().get(seat.seatTypeCode) ?? 0), 0),
  );

  private realtimeSub?: Subscription;

  load(showtimeId: number): void {
    this.detail.set(null);
    this.seatMap.set(null);
    this.notFound.set(false);
    this.selectedIds.set(new Set());
    forkJoin({
      detail: this.catalogApi.getShowtime(showtimeId),
      seatMap: this.catalogApi.getSeatMap(showtimeId),
    }).subscribe({
      next: ({ detail, seatMap }) => {
        this.detail.set(detail);
        this.seatMap.set(seatMap);
        this.listen(showtimeId);
      },
      error: () => this.notFound.set(true),
    });
  }

  toggle(seat: SeatMapSeat): void {
    this.selectedIds.update((ids) => {
      const next = new Set(ids);
      if (!next.delete(seat.id)) {
        next.add(seat.id);
      }
      return next;
    });
  }

  clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  /** Giữ các ghế đã chọn rồi tạo đơn chờ thanh toán. */
  book(): Observable<{ order: Order; warnings: string[] }> {
    const showtimeId = this.seatMap()!.showtimeId;
    const seatIds = this.selectedSeats().map((s) => s.id);
    this.submitting.set(true);
    let warnings: string[] = [];
    return this.bookingApi.hold(showtimeId, seatIds).pipe(
      tap((hold) => (warnings = hold.warnings)),
      switchMap((hold) => this.bookingApi.createOrder(hold.id)),
      map((order) => ({ order, warnings })),
      finalize(() => this.submitting.set(false)),
    );
  }

  /** Lỗi khi giữ ghế: ghế vừa bị người khác giữ → tải lại sơ đồ để thấy trạng thái mới. */
  handleBookingError(error: unknown): BookingError {
    if (!(error instanceof HttpErrorResponse)) {
      return { code: null, message: 'Đã có lỗi xảy ra, vui lòng thử lại' };
    }
    const code = (error.error as ApiResponse<unknown> | null)?.error?.code ?? null;
    if (code === 'SEAT_UNAVAILABLE' || code === 'CONFLICT') {
      this.refreshSeatMap();
    }
    return { code, message: resolveErrorMessage(error) };
  }

  private listen(showtimeId: number): void {
    this.realtimeSub?.unsubscribe();
    this.realtimeSub = new Subscription();
    this.realtimeSub.add(
      this.realtime
        .watch<SeatsChangedMessage>(`/topic/showtimes/${showtimeId}/seats`)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((message) => this.applyChanges(message)),
    );
    // Sau khi kết nối lại có thể đã lỡ thông điệp → đồng bộ lại toàn bộ sơ đồ
    let first = true;
    this.realtimeSub.add(
      this.realtime.connected$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
        if (!first) {
          this.refreshSeatMap();
        }
        first = false;
      }),
    );
  }

  private refreshSeatMap(): void {
    const map = this.seatMap();
    if (!map) {
      return;
    }
    this.catalogApi.getSeatMap(map.showtimeId).subscribe((seatMap) => {
      this.seatMap.set(seatMap);
      this.dropUnavailableSelections();
    });
  }

  private applyChanges(message: SeatsChangedMessage): void {
    const changes = new Map(message.seats.map((s) => [s.id, s.status]));
    this.seatMap.update((map) =>
      map
        ? { ...map, seats: map.seats.map((s) => (changes.has(s.id) ? { ...s, status: changes.get(s.id)! } : s)) }
        : map,
    );
    if (!this.submitting()) {
      this.dropUnavailableSelections();
    }
  }

  /** Bỏ chọn ghế vừa bị người khác giữ/bán. */
  private dropUnavailableSelections(): void {
    const available = new Set(
      (this.seatMap()?.seats ?? []).filter((s) => s.status === 'AVAILABLE').map((s) => s.id),
    );
    const ids = this.selectedIds();
    if ([...ids].some((id) => !available.has(id))) {
      this.selectedIds.set(new Set([...ids].filter((id) => available.has(id))));
    }
  }
}
