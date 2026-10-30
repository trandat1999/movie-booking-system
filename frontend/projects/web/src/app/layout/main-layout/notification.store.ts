import { DestroyRef, effect, inject, Injectable, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AppNotification, AuthStore, NotificationApi, RealtimeService } from '@mb/shared';
import { Subject, Subscription } from 'rxjs';

const LATEST_SIZE = 8;

/** Chuông thông báo trên header (NTF01): số chưa đọc, thông báo mới nhất, nhận realtime. */
@Injectable()
export class NotificationStore {
  private readonly api = inject(NotificationApi);
  private readonly authStore = inject(AuthStore);
  private readonly realtime = inject(RealtimeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly latest = signal<AppNotification[]>([]);
  readonly unread = signal(0);

  /** Thông báo vừa đến qua realtime — layout hiện toast */
  readonly arrived$ = new Subject<AppNotification>();

  private sub?: Subscription;

  constructor() {
    effect(() => {
      const userId = this.authStore.user()?.id ?? null;
      untracked(() => (userId === null ? this.reset() : this.start()));
    });
  }

  refresh(): void {
    this.api.list(0, LATEST_SIZE).subscribe((page) => this.latest.set(page.items));
    this.api.unreadCount().subscribe((count) => this.unread.set(count));
  }

  markRead(notification: AppNotification): void {
    if (notification.readAt) {
      return;
    }
    this.api.markRead(notification.id).subscribe(() => {
      this.latest.update((list) =>
        list.map((n) => (n.id === notification.id ? { ...n, readAt: new Date().toISOString() } : n)),
      );
      this.unread.update((count) => Math.max(0, count - 1));
    });
  }

  markAllRead(): void {
    this.api.markAllRead().subscribe(() => {
      const now = new Date().toISOString();
      this.latest.update((list) => list.map((n) => ({ ...n, readAt: n.readAt ?? now })));
      this.unread.set(0);
    });
  }

  private start(): void {
    this.refresh();
    this.sub?.unsubscribe();
    this.sub = this.realtime
      .watch<AppNotification>('/user/queue/notifications')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((notification) => {
        this.latest.update((list) => [notification, ...list].slice(0, LATEST_SIZE));
        this.unread.update((count) => count + 1);
        this.arrived$.next(notification);
      });
  }

  private reset(): void {
    this.sub?.unsubscribe();
    this.latest.set([]);
    this.unread.set(0);
  }
}
