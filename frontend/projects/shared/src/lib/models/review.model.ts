import { PageResponse } from './api-response.model';

/** Đánh giá phim (FBK01). */
export interface ReviewView {
  id: number;
  movieId: number;
  movieTitle: string;
  userId: number;
  userName: string;
  /** 1–5 sao */
  rating: number;
  comment: string | null;
  hidden: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface MovieReviews {
  /** 0 khi chưa có đánh giá */
  average: number;
  count: number;
  /** Số đánh giá theo mức sao: phần tử 0 = 1 sao … phần tử 4 = 5 sao */
  distribution: number[];
  items: PageResponse<ReviewView>;
}

export interface MyReview {
  /** Đã xem phim tại rạp (vé đã soát) nên được viết đánh giá */
  eligible: boolean;
  review: ReviewView | null;
}

export interface ReviewRequest {
  rating: number;
  comment: string | null;
}
