import { Component, computed, DestroyRef, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AGE_RATING_COLORS, MoviePosterComponent } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { MovieCardComponent } from '../../../shared/components/movie-card.component';
import { HomeStore } from '../home.store';

/** Trang chủ (MOV01): banner phim nổi bật, phim đang chiếu, phim sắp chiếu. */
@Component({
  selector: 'app-home-page',
  imports: [RouterLink, NzButtonModule, NzTagModule, NzSkeletonModule, MoviePosterComponent, MovieCardComponent],
  templateUrl: './home.page.html',
  styleUrl: './home.page.less',
})
export class HomePage implements OnInit {
  protected readonly store = inject(HomeStore);
  protected readonly ratingColors = AGE_RATING_COLORS;

  /** Phim nổi bật: 4 phim đang chiếu mới khởi chiếu nhất */
  protected readonly featured = computed(() => this.store.nowShowing().slice(0, 4));

  protected readonly activeSlide = signal(0);
  private readonly hero = viewChild<ElementRef<HTMLElement>>('hero');

  constructor() {
    // Tự chuyển banner mỗi 5 giây
    const timer = setInterval(() => {
      const count = this.featured().length;
      if (count > 1) {
        this.goTo((this.activeSlide() + 1) % count);
      }
    }, 5000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  ngOnInit(): void {
    this.store.load();
  }

  protected goTo(index: number): void {
    const el = this.hero()?.nativeElement;
    if (el) {
      el.scrollTo({ left: index * el.clientWidth, behavior: 'smooth' });
    }
    this.activeSlide.set(index);
  }
}
