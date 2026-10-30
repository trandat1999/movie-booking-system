import { inject, Injectable, signal } from '@angular/core';
import {
  CatalogApi,
  Cinema,
  CreateGroupRequest,
  GroupApi,
  GroupDetail,
  GroupListItem,
  InvitePreview,
  MovieSummary,
} from '@mb/shared';
import { finalize, forkJoin, Observable } from 'rxjs';

/** Nhóm của tôi, tạo nhóm, tham gia bằng mã mời (GRP01, GRP02, GRP20). */
@Injectable()
export class MyGroupsStore {
  private readonly api = inject(GroupApi);
  private readonly catalogApi = inject(CatalogApi);

  readonly groups = signal<GroupListItem[] | null>(null);
  readonly movies = signal<MovieSummary[]>([]);
  readonly cinemas = signal<Cinema[]>([]);
  readonly invite = signal<InvitePreview | null>(null);
  readonly inviteError = signal(false);
  readonly busy = signal(false);

  loadGroups(): void {
    this.api.mine().subscribe((groups) => this.groups.set(groups));
  }

  /** Phim đang/sắp chiếu và rạp cho form tạo nhóm. */
  loadCreateOptions(): void {
    forkJoin({
      now: this.catalogApi.searchMovies({ status: 'NOW_SHOWING', page: 0, size: 100 }),
      soon: this.catalogApi.searchMovies({ status: 'COMING_SOON', page: 0, size: 100 }),
      cinemas: this.catalogApi.listCinemas(),
    }).subscribe(({ now, soon, cinemas }) => {
      this.movies.set([...now.items, ...soon.items]);
      this.cinemas.set(cinemas);
    });
  }

  create(request: CreateGroupRequest): Observable<GroupDetail> {
    this.busy.set(true);
    return this.api.create(request).pipe(finalize(() => this.busy.set(false)));
  }

  loadInvite(code: string): void {
    this.invite.set(null);
    this.inviteError.set(false);
    this.api.previewInvite(code).subscribe({
      next: (preview) => this.invite.set(preview),
      error: () => this.inviteError.set(true),
    });
  }

  join(code: string): Observable<GroupDetail> {
    this.busy.set(true);
    return this.api.join(code).pipe(finalize(() => this.busy.set(false)));
  }
}
