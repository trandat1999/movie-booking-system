import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AGE_RATING_COLORS,
  MOVIE_STATUS_COLORS,
  MOVIE_STATUS_LABELS,
  MovieStatus,
  MovieSummary,
  toOptions,
} from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { MoviePosterComponent } from '../components/movie-poster.component';
import { CatalogStore } from '../catalog.store';

@Component({
  selector: 'adm-movies-page',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    NzCardModule,
    NzTableModule,
    NzInputModule,
    NzSelectModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    NzPopconfirmModule,
    MoviePosterComponent,
  ],
  templateUrl: './movies.page.html',
  styleUrl: './movies.page.less',
})
export class MoviesPage implements OnInit {
  protected readonly store = inject(CatalogStore);
  private readonly message = inject(NzMessageService);

  protected readonly statusOptions = toOptions(MOVIE_STATUS_LABELS);
  protected readonly statusLabels = MOVIE_STATUS_LABELS;
  protected readonly statusColors = MOVIE_STATUS_COLORS;
  protected readonly ratingColors = AGE_RATING_COLORS;

  protected keyword = '';
  protected status: MovieStatus | null = null;
  protected genreId: number | null = null;

  ngOnInit(): void {
    this.store.loadMovies();
    this.store.loadGenres();
  }

  protected search(): void {
    this.store.searchMovies({ q: this.keyword.trim() || null, status: this.status, genreId: this.genreId });
  }

  protected remove(movie: MovieSummary): void {
    this.store.deleteMovie(movie.id).subscribe(() => this.message.success(`Đã xóa phim ${movie.title}`));
  }
}
