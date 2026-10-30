import { Component, effect, inject, input, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { toIsoDate, touchAll } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';

import { MyGroupsStore } from '../my-groups.store';

/** Tạo nhóm (GRP01): tên, phim, rạp (tùy chọn), khoảng ngày dự kiến, hạn chốt. */
@Component({
  selector: 'app-group-create-page',
  imports: [
    ReactiveFormsModule,
    NzButtonModule,
    NzCardModule,
    NzDatePickerModule,
    NzFormModule,
    NzInputModule,
    NzSelectModule,
  ],
  template: `
    <nz-card nzTitle="Tạo nhóm đặt vé">
      <form nz-form nzLayout="vertical" [formGroup]="form" (ngSubmit)="submit()">
        <nz-form-item>
          <nz-form-label nzFor="name" nzRequired>Tên nhóm</nz-form-label>
          <nz-form-control nzErrorTip="Nhập tên nhóm (tối đa 120 ký tự)">
            <input id="name" nz-input formControlName="name" placeholder="Ví dụ: Lớp K65 đi xem phim" />
          </nz-form-control>
        </nz-form-item>
        <nz-form-item>
          <nz-form-label nzFor="movieId" nzRequired>Phim</nz-form-label>
          <nz-form-control nzErrorTip="Chọn phim">
            <nz-select id="movieId" formControlName="movieId" nzShowSearch nzPlaceHolder="Chọn phim">
              @for (m of store.movies(); track m.id) {
                <nz-option [nzValue]="m.id" [nzLabel]="m.title" />
              }
            </nz-select>
          </nz-form-control>
        </nz-form-item>
        <nz-form-item>
          <nz-form-label nzFor="cinemaId">Rạp (để trống nếu rạp nào cũng được)</nz-form-label>
          <nz-form-control>
            <nz-select id="cinemaId" formControlName="cinemaId" nzAllowClear nzPlaceHolder="Rạp nào cũng được">
              @for (c of store.cinemas(); track c.id) {
                <nz-option [nzValue]="c.id" [nzLabel]="c.name" />
              }
            </nz-select>
          </nz-form-control>
        </nz-form-item>
        <div class="row">
          <nz-form-item>
            <nz-form-label nzFor="dateFrom">Từ ngày</nz-form-label>
            <nz-form-control>
              <nz-date-picker id="dateFrom" formControlName="dateFrom" nzFormat="dd/MM/yyyy" class="full" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="dateTo">Đến ngày</nz-form-label>
            <nz-form-control>
              <nz-date-picker id="dateTo" formControlName="dateTo" nzFormat="dd/MM/yyyy" class="full" />
            </nz-form-control>
          </nz-form-item>
        </div>
        <nz-form-item>
          <nz-form-label nzFor="rsvpDeadline" nzRequired>Hạn chốt</nz-form-label>
          <nz-form-control nzErrorTip="Chọn hạn chốt trong tương lai" nzExtra="Quá hạn mà chưa chốt suất chiếu thì nhóm hết hạn.">
            <nz-date-picker
              id="rsvpDeadline"
              formControlName="rsvpDeadline"
              nzShowTime
              nzFormat="HH:mm dd/MM/yyyy"
              class="full"
            />
          </nz-form-control>
        </nz-form-item>
        <button nz-button nzType="primary" nzSize="large" type="submit" [nzLoading]="store.busy()">Tạo nhóm</button>
      </form>
    </nz-card>
  `,
  styles: `
    nz-card {
      max-width: 640px;
      margin: 0 auto;
    }

    .row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .full {
      width: 100%;
    }
  `,
})
export class GroupCreatePage implements OnInit {
  protected readonly store = inject(MyGroupsStore);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  /** ?movieId=... từ trang phim */
  readonly movieId = input<string>();

  protected readonly form = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    movieId: this.fb.control<number | null>(null, Validators.required),
    cinemaId: this.fb.control<number | null>(null),
    dateFrom: this.fb.control<Date | null>(null),
    dateTo: this.fb.control<Date | null>(null),
    rsvpDeadline: this.fb.control<Date | null>(defaultDeadline(), Validators.required),
  });

  constructor() {
    effect(() => {
      const id = Number(this.movieId());
      if (id) {
        this.form.controls.movieId.setValue(id);
      }
    });
  }

  ngOnInit(): void {
    this.store.loadCreateOptions();
  }

  protected submit(): void {
    const value = this.form.getRawValue();
    if (this.form.invalid || !value.rsvpDeadline || value.rsvpDeadline.getTime() <= Date.now()) {
      if (value.rsvpDeadline && value.rsvpDeadline.getTime() <= Date.now()) {
        this.form.controls.rsvpDeadline.setErrors({ past: true });
      }
      touchAll(this.form);
      return;
    }
    this.store
      .create({
        name: value.name.trim(),
        movieId: value.movieId!,
        cinemaId: value.cinemaId,
        dateFrom: toIsoDate(value.dateFrom),
        dateTo: toIsoDate(value.dateTo),
        rsvpDeadline: value.rsvpDeadline.toISOString(),
      })
      .subscribe((group) => void this.router.navigate(['/groups', group.id]));
  }
}

/** Mặc định hạn chốt sau 2 ngày, 20:00. */
function defaultDeadline(): Date {
  const date = new Date();
  date.setDate(date.getDate() + 2);
  date.setHours(20, 0, 0, 0);
  return date;
}
