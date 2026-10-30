import { inject, Injectable, signal } from '@angular/core';
import {
  AdminCinemaApi,
  Cinema,
  CinemaRequest,
  Room,
  RoomLayout,
  RoomLayoutRequest,
  RoomRequest,
  Seat,
} from '@mb/shared';
import { finalize, Observable, tap } from 'rxjs';

/** Trạng thái các trang rạp, phòng chiếu và sơ đồ ghế (CIN). */
@Injectable()
export class CinemasStore {
  private readonly api = inject(AdminCinemaApi);

  readonly cinemas = signal<Cinema[]>([]);
  readonly cinema = signal<Cinema | null>(null);
  readonly rooms = signal<Room[]>([]);
  readonly layout = signal<RoomLayout | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);

  loadCinemas(): void {
    this.loading.set(true);
    this.api
      .listCinemas()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((cinemas) => this.cinemas.set(cinemas));
  }

  saveCinema(id: number | null, request: CinemaRequest): Observable<Cinema> {
    this.saving.set(true);
    const call = id === null ? this.api.createCinema(request) : this.api.updateCinema(id, request);
    return call.pipe(
      tap((cinema) => {
        this.loadCinemas();
        if (this.cinema()?.id === cinema.id) {
          this.cinema.set(cinema);
        }
      }),
      finalize(() => this.saving.set(false)),
    );
  }

  loadCinema(id: number): void {
    this.loading.set(true);
    this.api.getCinema(id).subscribe((cinema) => this.cinema.set(cinema));
    this.api
      .listRooms(id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((rooms) => this.rooms.set(rooms));
  }

  saveRoom(cinemaId: number, roomId: number | null, request: RoomRequest): Observable<Room> {
    this.saving.set(true);
    const call = roomId === null ? this.api.createRoom(cinemaId, request) : this.api.updateRoom(roomId, request);
    return call.pipe(
      tap(() => this.loadCinema(cinemaId)),
      finalize(() => this.saving.set(false)),
    );
  }

  loadLayout(roomId: number): void {
    this.loading.set(true);
    this.api
      .getLayout(roomId)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((layout) => this.layout.set(layout));
  }

  saveLayout(roomId: number, request: RoomLayoutRequest): Observable<RoomLayout> {
    this.saving.set(true);
    return this.api.saveLayout(roomId, request).pipe(
      tap((layout) => this.layout.set(layout)),
      finalize(() => this.saving.set(false)),
    );
  }

  setSeatActive(roomId: number, seat: Seat, active: boolean): Observable<Seat> {
    return this.api.setSeatActive(roomId, seat.id, active).pipe(
      tap((updated) =>
        this.layout.update((layout) =>
          layout ? { ...layout, seats: layout.seats.map((s) => (s.id === updated.id ? updated : s)) } : layout,
        ),
      ),
    );
  }
}
