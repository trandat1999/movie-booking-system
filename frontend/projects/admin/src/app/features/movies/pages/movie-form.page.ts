import { Component, inject, input, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import {
  AGE_RATING_LABELS,
  AgeRating,
  applyServerErrors,
  fromIsoDate,
  MOVIE_STATUS_LABELS,
  MovieDetail,
  MovieStatus,
  Person,
  toIsoDate,
  toOptions,
  touchAll,
} from '@mb/shared';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { debounceTime, distinctUntilChanged, Subject, switchMap } from 'rxjs';

import { MoviePosterComponent } from '../components/movie-poster.component';
import { CatalogStore } from '../catalog.store';

@Component({
  selector: 'adm-movie-form-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    NzCardModule,
    NzFormModule,
    NzGridModule,
    NzInputModule,
    NzInputNumberModule,
    NzSelectModule,
    NzDatePickerModule,
    NzButtonModule,
    NzAlertModule,
    NzSkeletonModule,
    MoviePosterComponent,
  ],
  templateUrl: './movie-form.page.html',
  styles: `
    .actions {
      display: flex;
      gap: 8px;
    }

    .alert {
      margin-bottom: 16px;
    }

    .full {
      width: 100%;
    }
  `,
})
export class MovieFormPage implements OnInit {
  protected readonly store = inject(CatalogStore);
  private readonly router = inject(Router);
  private readonly message = inject(NzMessageService);

  /** Tham số route :id (không có khi tạo mới) */
  readonly id = input<string>();

  protected readonly ratingOptions = toOptions(AGE_RATING_LABELS);
  protected readonly statusOptions = toOptions(MOVIE_STATUS_LABELS);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Lựa chọn cho ô đạo diễn/diễn viên: kết quả tìm kiếm + người đã chọn sẵn */
  protected readonly peopleOptions = signal<Person[]>([]);
  private readonly peopleSearch$ = new Subject<string>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    originalTitle: [''],
    description: [''],
    durationMin: [120, [Validators.required, Validators.min(1), Validators.max(600)]],
    ageRating: ['T13' as AgeRating, [Validators.required]],
    language: ['Tiếng Việt', [Validators.required]],
    releaseDate: [null as Date | null, [Validators.required]],
    posterUrl: [''],
    bannerUrl: [''],
    trailerUrl: [''],
    status: ['COMING_SOON' as MovieStatus, [Validators.required]],
    genreIds: [[] as number[], [Validators.required]],
    directorIds: [[] as number[]],
    actorIds: [[] as number[]],
  });

  protected get isEdit(): boolean {
    return !!this.id();
  }

  constructor() {
    this.peopleSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((q) => this.store.lookupPeople(q)),
      )
      .subscribe((result) => this.mergePeopleOptions(result.items));
  }

  ngOnInit(): void {
    this.store.loadGenres();
    this.peopleSearch$.next('');
    const id = this.id();
    if (id) {
      this.loading.set(true);
      this.store.getMovie(Number(id)).subscribe({
        next: (movie) => {
          this.fill(movie);
          this.loading.set(false);
        },
        error: () => void this.router.navigate(['/movies']),
      });
    }
  }

  protected searchPeople(q: string): void {
    this.peopleSearch$.next(q);
  }

  protected submit(): void {
    if (this.form.invalid) {
      touchAll(this.form);
      return;
    }
    const v = this.form.getRawValue();
    this.error.set(null);
    this.store
      .saveMovie(this.id() ? Number(this.id()) : null, {
        title: v.title.trim(),
        originalTitle: v.originalTitle.trim() || null,
        description: v.description.trim() || null,
        durationMin: v.durationMin,
        ageRating: v.ageRating,
        language: v.language.trim(),
        releaseDate: toIsoDate(v.releaseDate)!,
        posterUrl: v.posterUrl.trim() || null,
        bannerUrl: v.bannerUrl.trim() || null,
        trailerUrl: v.trailerUrl.trim() || null,
        status: v.status,
        genreIds: v.genreIds,
        directorIds: v.directorIds,
        actorIds: v.actorIds,
      })
      .subscribe({
        next: (movie) => {
          this.message.success(this.isEdit ? 'Đã cập nhật phim' : `Đã thêm phim ${movie.title}`);
          void this.router.navigate(['/movies']);
        },
        error: (err: unknown) => this.error.set(applyServerErrors(this.form, err)),
      });
  }

  private fill(movie: MovieDetail): void {
    this.mergePeopleOptions([...movie.directors, ...movie.actors]);
    this.form.setValue({
      title: movie.title,
      originalTitle: movie.originalTitle ?? '',
      description: movie.description ?? '',
      durationMin: movie.durationMin,
      ageRating: movie.ageRating,
      language: movie.language,
      releaseDate: fromIsoDate(movie.releaseDate),
      posterUrl: movie.posterUrl ?? '',
      bannerUrl: movie.bannerUrl ?? '',
      trailerUrl: movie.trailerUrl ?? '',
      status: movie.status,
      genreIds: movie.genres.map((g) => g.id),
      directorIds: movie.directors.map((p) => p.id),
      actorIds: movie.actors.map((p) => p.id),
    });
  }

  /** Giữ người đã chọn trong danh sách lựa chọn để nhãn không bị mất khi kết quả tìm kiếm thay đổi. */
  private mergePeopleOptions(found: Person[]): void {
    const selected = new Set([...this.form.controls.directorIds.value, ...this.form.controls.actorIds.value]);
    const byId = new Map<number, Person>();
    this.peopleOptions()
      .filter((p) => selected.has(p.id))
      .forEach((p) => byId.set(p.id, p));
    found.forEach((p) => byId.set(p.id, p));
    this.peopleOptions.set([...byId.values()]);
  }
}
