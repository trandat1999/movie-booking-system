import { inject, Injectable, signal } from '@angular/core';
import { AdminCinemaApi, AdminShowtimeApi, BasePrice, DayType, Holiday, ScreenFormat, SeatType } from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Bảng giá cơ bản, phụ thu loại ghế, ngày lễ (SCHADM06, CIN05). */
@Injectable()
export class PricingStore {
  private readonly showtimeApi = inject(AdminShowtimeApi);
  private readonly cinemaApi = inject(AdminCinemaApi);

  readonly basePrices = signal<BasePrice[]>([]);
  readonly seatTypes = signal<SeatType[]>([]);
  readonly holidays = signal<Holiday[]>([]);
  readonly year = signal(new Date().getFullYear());
  readonly saving = signal(false);

  load(): void {
    this.showtimeApi.listBasePrices().subscribe((prices) => this.basePrices.set(prices));
    this.cinemaApi.listSeatTypes().subscribe((types) => this.seatTypes.set(types));
    this.loadHolidays();
  }

  loadHolidays(): void {
    this.showtimeApi.listHolidays(this.year()).subscribe((holidays) => this.holidays.set(holidays));
  }

  changeYear(delta: number): void {
    this.year.update((y) => y + delta);
    this.loadHolidays();
  }

  saveBasePrices(items: { format: ScreenFormat; dayType: DayType; price: number }[]): Observable<BasePrice[]> {
    this.saving.set(true);
    return this.showtimeApi.updateBasePrices(items).pipe(
      tap((prices) => this.basePrices.set(prices)),
      finalize(() => this.saving.set(false)),
    );
  }

  saveSeatType(type: SeatType): Observable<SeatType> {
    return this.cinemaApi
      .updateSeatType(type.code, { name: type.name, surcharge: type.surcharge, color: type.color })
      .pipe(tap((updated) => this.seatTypes.update((types) => types.map((t) => (t.code === updated.code ? updated : t)))));
  }

  addHoliday(holiday: Holiday): Observable<Holiday> {
    return this.showtimeApi.addHoliday(holiday).pipe(tap(() => this.loadHolidays()));
  }

  deleteHoliday(date: string): Observable<void> {
    return this.showtimeApi.deleteHoliday(date).pipe(tap(() => this.loadHolidays()));
  }
}
