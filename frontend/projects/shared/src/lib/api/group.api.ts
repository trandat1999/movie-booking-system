import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiClient } from '../core/http/api-client';
import { skipErrorMessage } from '../core/http/http-context';
import { Order } from '../models/booking.model';
import {
  CreateGroupRequest,
  GroupDetail,
  GroupListItem,
  GroupSummary,
  InvitePreview,
  RsvpRequest,
  SeatSuggestions,
  SeatSuggestionView,
  SuggestionOptions,
} from '../models/group.model';

/** Đặt vé theo nhóm (M6). */
@Injectable({ providedIn: 'root' })
export class GroupApi {
  private readonly api = inject(ApiClient);

  create(request: CreateGroupRequest): Observable<GroupDetail> {
    return this.api.post<GroupDetail>('/groups', request);
  }

  mine(): Observable<GroupListItem[]> {
    return this.api.get<GroupListItem[]>('/me/groups');
  }

  /** Trang chi tiết tự hiển thị "không tìm thấy" nên không hiện thông báo lỗi chung. */
  get(id: number): Observable<GroupDetail> {
    return this.api.get<GroupDetail>(`/groups/${id}`, { context: skipErrorMessage() });
  }

  update(id: number, request: Omit<CreateGroupRequest, 'movieId'>): Observable<GroupDetail> {
    return this.api.patch<GroupDetail>(`/groups/${id}`, request);
  }

  previewInvite(code: string): Observable<InvitePreview> {
    return this.api.get<InvitePreview>(`/groups/invite/${encodeURIComponent(code)}`, { context: skipErrorMessage() });
  }

  join(code: string): Observable<GroupDetail> {
    return this.api.post<GroupDetail>('/groups/join', { code });
  }

  leave(id: number): Observable<void> {
    return this.api.post<void>(`/groups/${id}/leave`);
  }

  removeMember(id: number, userId: number): Observable<GroupDetail> {
    return this.api.delete<GroupDetail>(`/groups/${id}/members/${userId}`);
  }

  transferLeader(id: number, userId: number): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(`/groups/${id}/transfer-leader`, { userId });
  }

  regenerateCode(id: number): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(`/groups/${id}/invite-code/regenerate`);
  }

  rsvp(id: number, request: RsvpRequest): Observable<GroupDetail> {
    return this.api.put<GroupDetail>(`/groups/${id}/rsvp`, request);
  }

  propose(id: number, showtimeId: number): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(`/groups/${id}/options`, { showtimeId });
  }

  removeOption(id: number, optionId: number): Observable<GroupDetail> {
    return this.api.delete<GroupDetail>(`/groups/${id}/options/${optionId}`);
  }

  vote(id: number, optionId: number, voted: boolean): Observable<GroupDetail> {
    const url = `/groups/${id}/options/${optionId}/vote`;
    return voted ? this.api.post<GroupDetail>(url) : this.api.delete<GroupDetail>(url);
  }

  summary(id: number): Observable<GroupSummary> {
    return this.api.get<GroupSummary>(`/groups/${id}/summary`);
  }

  lock(id: number, showtimeId: number, ticketCount: number | null): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(`/groups/${id}/lock`, { showtimeId, ticketCount });
  }

  unlock(id: number): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(`/groups/${id}/unlock`);
  }

  cancel(id: number): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(`/groups/${id}/cancel`);
  }

  suggestSeats(id: number, options: SuggestionOptions): Observable<SeatSuggestions> {
    return this.api.post<SeatSuggestions>(`/groups/${id}/seat-suggestions`, options);
  }

  evaluateSeats(id: number, seatIds: number[]): Observable<SeatSuggestionView> {
    return this.api.post<SeatSuggestionView>(`/groups/${id}/seat-evaluation`, { seatIds }, { context: skipErrorMessage() });
  }

  previewSeats(id: number, seatIds: number[]): Observable<void> {
    return this.api.post<void>(`/groups/${id}/preview`, { seatIds }, { context: skipErrorMessage() });
  }

  /** Lỗi ghế vừa bị giữ (kèm phương án mới) do trang tự xử lý. */
  hold(id: number, seatIds: number[], suggestionLogId: number | null, chosenRank: number | null): Observable<GroupDetail> {
    return this.api.post<GroupDetail>(
      `/groups/${id}/hold`,
      { seatIds, suggestionLogId, chosenRank },
      { context: skipErrorMessage() },
    );
  }

  releaseHold(id: number): Observable<GroupDetail> {
    return this.api.delete<GroupDetail>(`/groups/${id}/hold`);
  }

  createOrder(id: number): Observable<Order> {
    return this.api.post<Order>(`/groups/${id}/order`);
  }

  updateAssignments(
    id: number,
    items: { ticketCode: string; userId: number; guestLabel: string | null }[],
  ): Observable<GroupDetail> {
    return this.api.put<GroupDetail>(`/groups/${id}/assignments`, { items });
  }
}
