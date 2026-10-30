import { Component, computed, input } from '@angular/core';

/** Poster phim; chưa có ảnh thì hiện khối màu với chữ cái đầu của tên phim. */
@Component({
  selector: 'adm-movie-poster',
  template: `
    @if (url()) {
      <img [src]="url()" [alt]="title()" [style.width.px]="width()" [style.height.px]="width() * 1.5" />
    } @else {
      <div class="placeholder" [style.width.px]="width()" [style.height.px]="width() * 1.5" [style.background]="color()">
        {{ initials() }}
      </div>
    }
  `,
  styles: `
    img,
    .placeholder {
      display: block;
      border-radius: 4px;
      object-fit: cover;
    }

    .placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-weight: 600;
    }
  `,
})
export class MoviePosterComponent {
  readonly url = input<string | null>(null);
  readonly title = input.required<string>();
  readonly width = input(40);

  protected readonly initials = computed(() =>
    this.title()
      .split(/\s+/)
      .slice(0, 2)
      .map((w) => w.charAt(0).toUpperCase())
      .join(''),
  );

  protected readonly color = computed(() => {
    let hash = 0;
    for (const ch of this.title()) {
      hash = (hash * 31 + ch.charCodeAt(0)) % 360;
    }
    return `hsl(${hash}, 55%, 45%)`;
  });
}
