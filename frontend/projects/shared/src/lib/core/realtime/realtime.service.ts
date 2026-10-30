import { computed, DestroyRef, effect, inject, Injectable, untracked } from '@angular/core';
import { RxStomp } from '@stomp/rx-stomp';
import { map, Observable } from 'rxjs';

import { AuthStore } from '../auth/auth.store';

/**
 * Kết nối STOMP qua WebSocket tới `/ws` (PLAN.md 8.3). Một kết nối cho cả ứng dụng; tự kết nối lại khi mất mạng
 * và khi người dùng đăng nhập/đăng xuất (để server gắn đúng người dùng cho các kênh `/user/queue/...`).
 * Subscription của {@link watch} được rx-stomp tự đăng ký lại sau khi kết nối lại.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private readonly authStore = inject(AuthStore);
  private readonly stomp = new RxStomp();
  private readonly userId = computed(() => this.authStore.user()?.id ?? null);

  readonly connected$ = this.stomp.connected$;

  constructor() {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    this.stomp.configure({
      brokerURL: `${protocol}//${location.host}/ws`,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      // Token đọc lúc (kết nối lại) để luôn dùng access token mới nhất
      beforeConnect: (client) => {
        const token = this.authStore.accessToken();
        client.configure({ connectHeaders: token ? { Authorization: `Bearer ${token}` } : {} });
      },
    });
    this.stomp.activate();

    let previousUser: number | null | undefined;
    effect(() => {
      const user = this.userId();
      untracked(() => {
        if (previousUser !== undefined && previousUser !== user) {
          void this.reconnect();
        }
        previousUser = user;
      });
    });
    inject(DestroyRef).onDestroy(() => void this.stomp.deactivate());
  }

  /** Nhận thông điệp JSON từ một kênh, ví dụ `/topic/showtimes/1/seats`, `/user/queue/notifications`. */
  watch<T>(destination: string): Observable<T> {
    return this.stomp.watch(destination).pipe(map((message) => JSON.parse(message.body) as T));
  }

  private async reconnect(): Promise<void> {
    await this.stomp.deactivate();
    this.stomp.activate();
  }
}
