import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';

import { MovieCardComponent } from '../../../shared/components/movie-card.component';
import { FavoritesStore } from '../favorites.store';

/** Phim yêu thích (MOV04). */
@Component({
  selector: 'app-my-favorites-page',
  imports: [RouterLink, NzButtonModule, NzCardModule, NzEmptyModule, NzSkeletonModule, MovieCardComponent],
  template: `
    <nz-card nzTitle="Phim yêu thích">
      @if (store.loading()) {
        <nz-skeleton [nzActive]="true" />
      } @else {
        <div class="grid">
          @for (m of store.movies(); track m.id) {
            <app-movie-card [movie]="m" [showRelease]="m.status === 'COMING_SOON'" />
          }
        </div>
        @if (store.movies().length === 0) {
          <nz-empty nzNotFoundContent="Bạn chưa có phim yêu thích nào" [nzNotFoundFooter]="browse" />
        }
      }
    </nz-card>
    <ng-template #browse><a nz-button nzType="primary" routerLink="/movies">Khám phá phim</a></ng-template>
  `,
  styles: `
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 16px;
    }

    @media (max-width: 576px) {
      .grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `,
})
export class MyFavoritesPage implements OnInit {
  protected readonly store = inject(FavoritesStore);

  ngOnInit(): void {
    this.store.load();
  }
}
