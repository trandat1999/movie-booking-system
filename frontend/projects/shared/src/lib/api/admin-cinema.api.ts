import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import {
  Cinema,
  CinemaRequest,
  Room,
  RoomLayout,
  RoomLayoutRequest,
  RoomRequest,
  Seat,
  SeatType,
} from '../models/catalog.model';

/** Quản trị rạp, phòng chiếu, sơ đồ ghế, loại ghế. */
@Injectable({ providedIn: 'root' })
export class AdminCinemaApi {
  private readonly api = inject(ApiClient);

  listCinemas(): Observable<Cinema[]> {
    return this.api.get<Cinema[]>('/admin/cinemas');
  }

  getCinema(id: number): Observable<Cinema> {
    return this.api.get<Cinema>(`/admin/cinemas/${id}`);
  }

  createCinema(request: CinemaRequest): Observable<Cinema> {
    return this.api.post<Cinema>('/admin/cinemas', request);
  }

  updateCinema(id: number, request: CinemaRequest): Observable<Cinema> {
    return this.api.put<Cinema>(`/admin/cinemas/${id}`, request);
  }

  listRooms(cinemaId: number): Observable<Room[]> {
    return this.api.get<Room[]>(`/admin/cinemas/${cinemaId}/rooms`);
  }

  createRoom(cinemaId: number, request: RoomRequest): Observable<Room> {
    return this.api.post<Room>(`/admin/cinemas/${cinemaId}/rooms`, request);
  }

  updateRoom(roomId: number, request: RoomRequest): Observable<Room> {
    return this.api.put<Room>(`/admin/rooms/${roomId}`, request);
  }

  getLayout(roomId: number): Observable<RoomLayout> {
    return this.api.get<RoomLayout>(`/admin/rooms/${roomId}`);
  }

  /** Lỗi sơ đồ (danh sách chi tiết) được trang thiết kế tự hiển thị. */
  saveLayout(roomId: number, request: RoomLayoutRequest): Observable<RoomLayout> {
    return this.api.put<RoomLayout>(`/admin/rooms/${roomId}/layout`, request, { context: skipErrorMessage() });
  }

  setSeatActive(roomId: number, seatId: number, active: boolean): Observable<Seat> {
    return this.api.patch<Seat>(`/admin/rooms/${roomId}/seats/${seatId}`, { active });
  }

  listSeatTypes(): Observable<SeatType[]> {
    return this.api.get<SeatType[]>('/admin/seat-types');
  }

  updateSeatType(code: string, request: Pick<SeatType, 'name' | 'surcharge' | 'color'>): Observable<SeatType> {
    return this.api.put<SeatType>(`/admin/seat-types/${code}`, request);
  }
}
