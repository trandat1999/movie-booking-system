import { DatePipe } from '@angular/common';
import { Component, effect, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MovieReviews, MyReview, ReviewRequest, VN_OFFSET } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzPaginationModule } from 'ng-zorro-antd/pagination';
import { NzProgressModule } from 'ng-zorro-antd/progress';
import { NzRateModule } from 'ng-zorro-antd/rate';

/** Đánh giá phim (FBK01): điểm trung bình, phân bố sao, danh sách, form của tôi khi đã xem phim. */
@Component({
  selector: 'app-movie-reviews',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    NzButtonModule,
    NzEmptyModule,
    NzInputModule,
    NzPaginationModule,
    NzProgressModule,
    NzRateModule,
  ],
  template: `
    @if (reviews(); as r) {
      <div class="summary">
        <div class="score">
          <div class="average">{{ r.count ? r.average.toFixed(1) : '–' }}</div>
          <nz-rate [ngModel]="r.average" nzAllowHalf nzDisabled />
          <div class="muted">{{ r.count }} đánh giá</div>
        </div>
        <div class="bars">
          @for (star of [5, 4, 3, 2, 1]; track star) {
            <div class="bar">
              <span>{{ star }} ★</span>
              <nz-progress
                [nzPercent]="r.count ? (r.distribution[star - 1] * 100) / r.count : 0"
                [nzShowInfo]="false"
                nzSize="small"
              />
              <span class="muted">{{ r.distribution[star - 1] }}</span>
            </div>
          }
        </div>
      </div>
    }

    <!-- Form của tôi -->
    @if (!loggedIn()) {
      <p class="muted"><a routerLink="/login">Đăng nhập</a> để đánh giá phim bạn đã xem.</p>
    }
    @if (loggedIn() && mine(); as m) {
      @if (m.eligible) {
        <form class="mine" (ngSubmit)="submit()">
          <strong>{{ m.review ? 'Đánh giá của bạn' : 'Bạn đã xem phim này — hãy để lại đánh giá' }}</strong>
          <nz-rate name="rating" [(ngModel)]="rating" aria-label="Số sao" />
          <textarea
            nz-input
            name="comment"
            [(ngModel)]="comment"
            rows="3"
            maxlength="1000"
            placeholder="Cảm nhận của bạn (không bắt buộc)"
            aria-label="Nhận xét"
          ></textarea>
          <div class="actions">
            <button nz-button nzType="primary" type="submit" [disabled]="rating < 1" [nzLoading]="saving()">
              {{ m.review ? 'Cập nhật' : 'Gửi đánh giá' }}
            </button>
            @if (m.review) {
              <button nz-button nzDanger type="button" (click)="remove.emit()">Xóa đánh giá</button>
            }
          </div>
        </form>
      } @else {
        <p class="muted">Bạn có thể đánh giá sau khi xem phim tại rạp (vé đã được soát).</p>
      }
    }

    @if (reviews(); as r) {
      @for (item of r.items.items; track item.id) {
        <div class="review">
          <div class="head">
            <strong>{{ item.userName }}</strong>
            <nz-rate [ngModel]="item.rating" nzDisabled class="small-rate" />
            <span class="muted">{{ item.createdAt | date: 'dd/MM/yyyy' : vn }}</span>
          </div>
          @if (item.comment) {
            <p>{{ item.comment }}</p>
          }
        </div>
      } @empty {
        <nz-empty nzNotFoundContent="Chưa có đánh giá nào" />
      }
      @if (r.items.totalPages > 1) {
        <nz-pagination
          [nzPageIndex]="r.items.page + 1"
          [nzTotal]="r.items.totalElements"
          [nzPageSize]="r.items.size"
          nzSize="small"
          (nzPageIndexChange)="pageChange.emit($event - 1)"
        />
      }
    }
  `,
  styles: `
    .summary {
      display: flex;
      flex-wrap: wrap;
      gap: 24px;
      align-items: center;
      margin-bottom: 16px;
    }

    .score {
      text-align: center;
    }

    .average {
      font-size: 40px;
      font-weight: 600;
      line-height: 1;
    }

    .bars {
      flex: 1;
      min-width: 220px;
      max-width: 360px;
    }

    .bar {
      display: grid;
      grid-template-columns: 36px 1fr 28px;
      align-items: center;
      gap: 8px;
    }

    .mine {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 12px;
      margin-bottom: 16px;
      background: #fafafa;
      border-radius: 8px;
    }

    .actions {
      display: flex;
      gap: 8px;
    }

    .review {
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      p {
        margin: 4px 0 0;
        white-space: pre-line;
      }
    }

    .head {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;
    }

    .small-rate {
      font-size: 13px;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
    }
  `,
})
export class MovieReviewsComponent {
  readonly reviews = input<MovieReviews | null>(null);
  readonly mine = input<MyReview | null>(null);
  readonly loggedIn = input(false);
  readonly saving = input(false);
  readonly save = output<ReviewRequest>();
  readonly remove = output<void>();
  readonly pageChange = output<number>();

  protected readonly vn = VN_OFFSET;
  protected rating = 0;
  protected comment = '';

  constructor() {
    effect(() => {
      const review = this.mine()?.review;
      this.rating = review?.rating ?? 0;
      this.comment = review?.comment ?? '';
    });
  }

  protected submit(): void {
    this.save.emit({ rating: this.rating, comment: this.comment.trim() || null });
  }
}
