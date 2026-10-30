import { DatePipe } from '@angular/common';
import { Component, inject, Injectable, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PageResponse, ReviewApi, ReviewView, VN_OFFSET } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzRateModule } from 'ng-zorro-antd/rate';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { finalize, Observable, tap } from 'rxjs';

type Visibility = 'ALL' | 'VISIBLE' | 'HIDDEN';

@Injectable()
export class ReviewsStore {
  private readonly api = inject(ReviewApi);

  readonly page = signal<PageResponse<ReviewView> | null>(null);
  readonly loading = signal(false);

  visibility: Visibility = 'ALL';

  load(page = 0): void {
    const hidden = this.visibility === 'ALL' ? null : this.visibility === 'HIDDEN';
    this.loading.set(true);
    this.api
      .adminReviews(hidden, page, 20)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => this.page.set(result));
  }

  setHidden(review: ReviewView, hidden: boolean): Observable<ReviewView> {
    return this.api.setHidden(review.id, hidden).pipe(
      tap((updated) =>
        this.page.update((p) => (p ? { ...p, items: p.items.map((r) => (r.id === updated.id ? updated : r)) } : p)),
      ),
    );
  }
}

/** Kiểm duyệt đánh giá phim (FBK01): ẩn / hiện đánh giá vi phạm. */
@Component({
  selector: 'adm-reviews-page',
  providers: [ReviewsStore],
  imports: [DatePipe, FormsModule, NzButtonModule, NzCardModule, NzRateModule, NzSelectModule, NzTableModule, NzTagModule],
  template: `
    <nz-card nzTitle="Đánh giá phim">
      <div class="filters">
        <nz-select [(ngModel)]="store.visibility" (ngModelChange)="store.load(0)" class="w200" aria-label="Trạng thái">
          <nz-option nzValue="ALL" nzLabel="Tất cả" />
          <nz-option nzValue="VISIBLE" nzLabel="Đang hiển thị" />
          <nz-option nzValue="HIDDEN" nzLabel="Đã ẩn" />
        </nz-select>
      </div>
      <nz-table
        #table
        [nzData]="store.page()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.page()?.totalElements ?? 0"
        [nzPageIndex]="(store.page()?.page ?? 0) + 1"
        [nzPageSize]="20"
        [nzLoading]="store.loading()"
        (nzPageIndexChange)="store.load($event - 1)"
        [nzScroll]="{ x: '860px' }"
      >
        <thead>
          <tr>
            <th>Lúc</th>
            <th>Người viết</th>
            <th>Phim</th>
            <th>Sao</th>
            <th>Nhận xét</th>
            <th>Trạng thái</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (r of table.data; track r.id) {
            <tr>
              <td>{{ r.createdAt | date: 'HH:mm dd/MM/yyyy' : vn }}</td>
              <td>{{ r.userName }}</td>
              <td>{{ r.movieTitle }}</td>
              <td><nz-rate [ngModel]="r.rating" nzDisabled class="rate" /></td>
              <td class="comment">{{ r.comment ?? '—' }}</td>
              <td>
                @if (r.hidden) {
                  <nz-tag nzColor="red">Đã ẩn</nz-tag>
                } @else {
                  <nz-tag nzColor="green">Hiển thị</nz-tag>
                }
              </td>
              <td>
                <button nz-button nzSize="small" [nzDanger]="!r.hidden" (click)="toggle(r)">
                  {{ r.hidden ? 'Hiện lại' : 'Ẩn' }}
                </button>
              </td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>
  `,
  styles: `
    .filters {
      margin-bottom: 16px;
    }

    .w200 {
      width: 200px;
    }

    .rate {
      font-size: 13px;
      white-space: nowrap;
    }

    .comment {
      max-width: 360px;
      white-space: pre-line;
    }
  `,
})
export class ReviewsPage implements OnInit {
  protected readonly store = inject(ReviewsStore);
  private readonly message = inject(NzMessageService);
  protected readonly vn = VN_OFFSET;

  ngOnInit(): void {
    this.store.load();
  }

  protected toggle(review: ReviewView): void {
    this.store
      .setHidden(review, !review.hidden)
      .subscribe((r) => this.message.success(r.hidden ? 'Đã ẩn đánh giá' : 'Đã hiện lại đánh giá'));
  }
}
