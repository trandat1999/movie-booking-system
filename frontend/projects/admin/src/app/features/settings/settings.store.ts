import { computed, inject, Injectable, signal } from '@angular/core';
import { AdminShowtimeApi, Setting } from '@mb/shared';
import { Observable, tap } from 'rxjs';

/** Tham số nghiệp vụ (SYS01), nhóm theo chủ đề để hiển thị. */
@Injectable()
export class SettingsStore {
  private readonly api = inject(AdminShowtimeApi);

  readonly settings = signal<Setting[]>([]);

  readonly groups = computed(() => {
    const groups = new Map<string, Setting[]>();
    for (const s of this.settings()) {
      groups.set(s.group, [...(groups.get(s.group) ?? []), s]);
    }
    return [...groups.entries()].map(([name, items]) => ({ name, items }));
  });

  load(): Observable<Setting[]> {
    return this.api.listSettings().pipe(tap((settings) => this.settings.set(settings)));
  }

  save(key: string, value: string): Observable<Setting> {
    return this.api
      .updateSetting(key, value)
      .pipe(tap((updated) => this.settings.update((list) => list.map((s) => (s.key === updated.key ? updated : s)))));
  }
}
