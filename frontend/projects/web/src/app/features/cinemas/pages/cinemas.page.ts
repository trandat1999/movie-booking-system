import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CinemasStore } from '../cinemas.store';

/** Danh sách rạp theo thành phố (SCH01). */
@Component({
  selector: 'app-cinemas-page',
  imports: [RouterLink, NzCardModule, NzTagModule, NzSkeletonModule],
  template: `
    <h1>Hệ thống rạp</h1>
    @if (store.loading()) {
      <nz-skeleton [nzActive]="true" />
    }
    @for (group of store.byCity(); track group.city) {
      <h2 class="city">{{ group.city }}</h2>
      <div class="grid">
        @for (c of group.cinemas; track c.id) {
          <a [routerLink]="[c.id]" class="link">
            <nz-card nzHoverable [nzTitle]="c.name">
              <p>{{ c.address }}</p>
              @if (c.phone) {
                <p class="muted">☎ {{ c.phone }}</p>
              }
              <div>
                @for (a of c.amenities; track a) {
                  <nz-tag>{{ a }}</nz-tag>
                }
              </div>
            </nz-card>
          </a>
        }
      </div>
    }
  `,
  styles: `
    .city {
      margin-top: 24px;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }

    .link {
      color: inherit;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }
  `,
})
export class CinemasPage implements OnInit {
  protected readonly store = inject(CinemasStore);

  ngOnInit(): void {
    this.store.loadCinemas();
  }
}
