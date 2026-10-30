import { DatePipe } from '@angular/common';
import { Component, inject, Injectable, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminReportApi, PageResponse, Promotion, PromotionRequest, touchAll, VN_OFFSET, VndPipe } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { finalize, Observable, tap } from 'rxjs';

@Injectable()
export class PromotionsStore {
  private readonly api = inject(AdminReportApi);

  readonly page = signal<PageResponse<Promotion> | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);

  load(page = 0): void {
    this.loading.set(true);
    this.api
      .promotions(null, page, 20)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe((result) => this.page.set(result));
  }

  save(id: number | null, request: PromotionRequest): Observable<Promotion> {
    this.saving.set(true);
    const call = id === null ? this.api.createPromotion(request) : this.api.updatePromotion(id, request);
    return call.pipe(
      tap(() => this.load(this.page()?.page ?? 0)),
      finalize(() => this.saving.set(false)),
    );
  }
}

/** Mã giảm giá (PROADM): theo % hoặc số tiền, đơn tối thiểu, **số vé tối thiểu (ưu đãi nhóm)**, giới hạn lượt. */
@Component({
  selector: 'adm-promotions-page',
  providers: [PromotionsStore],
  imports: [
    DatePipe,
    ReactiveFormsModule,
    VndPipe,
    NzButtonModule,
    NzCardModule,
    NzDatePickerModule,
    NzFormModule,
    NzInputModule,
    NzInputNumberModule,
    NzModalModule,
    NzSelectModule,
    NzTableModule,
    NzTagModule,
  ],
  template: `
    <nz-card nzTitle="Mã giảm giá" [nzExtra]="createButton">
      <nz-table
        #table
        [nzData]="store.page()?.items ?? []"
        [nzFrontPagination]="false"
        [nzTotal]="store.page()?.totalElements ?? 0"
        [nzPageIndex]="(store.page()?.page ?? 0) + 1"
        [nzPageSize]="20"
        [nzLoading]="store.loading()"
        (nzPageIndexChange)="store.load($event - 1)"
        [nzScroll]="{ x: '960px' }"
      >
        <thead>
          <tr>
            <th>Mã</th>
            <th>Ưu đãi</th>
            <th>Điều kiện</th>
            <th>Hiệu lực</th>
            <th>Đã dùng</th>
            <th>Trạng thái</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          @for (p of table.data; track p.id) {
            <tr>
              <td><code>{{ p.code }}</code><div class="muted">{{ p.name }}</div></td>
              <td>
                {{ p.type === 'PERCENT' ? p.value + ' %' : (p.value | vnd) }}
                @if (p.maxDiscount) {
                  <div class="muted">tối đa {{ p.maxDiscount | vnd }}</div>
                }
              </td>
              <td>
                @if (p.minTickets > 1) {
                  <nz-tag nzColor="purple">Từ {{ p.minTickets }} vé</nz-tag>
                }
                @if (p.minOrder > 0) {
                  <div class="muted">Đơn từ {{ p.minOrder | vnd }}</div>
                }
              </td>
              <td>{{ p.startsAt | date: 'dd/MM/yyyy' : vn }} – {{ p.endsAt | date: 'dd/MM/yyyy' : vn }}</td>
              <td>{{ p.usedCount }}{{ p.usageLimit ? ' / ' + p.usageLimit : '' }}</td>
              <td>
                <nz-tag [nzColor]="p.status === 'ACTIVE' ? 'green' : 'default'">
                  {{ p.status === 'ACTIVE' ? 'Đang bật' : 'Tắt' }}
                </nz-tag>
              </td>
              <td><button nz-button nzSize="small" (click)="edit(p)">Sửa</button></td>
            </tr>
          }
        </tbody>
      </nz-table>
    </nz-card>
    <ng-template #createButton>
      <button nz-button nzType="primary" (click)="create()">Tạo mã</button>
    </ng-template>

    <nz-modal
      [nzVisible]="open()"
      [nzTitle]="editingId === null ? 'Tạo mã giảm giá' : 'Sửa mã giảm giá'"
      nzOkText="Lưu"
      [nzOkLoading]="store.saving()"
      (nzOnOk)="submit()"
      (nzOnCancel)="open.set(false)"
      [nzWidth]="640"
    >
      <ng-container *nzModalContent>
        <form nz-form nzLayout="vertical" [formGroup]="form">
          <div class="grid">
            <nz-form-item>
              <nz-form-label nzFor="code" nzRequired>Mã</nz-form-label>
              <nz-form-control nzErrorTip="Chữ, số, - và _">
                <input id="code" nz-input formControlName="code" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="name" nzRequired>Tên</nz-form-label>
              <nz-form-control nzErrorTip="Nhập tên">
                <input id="name" nz-input formControlName="name" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="type" nzRequired>Loại</nz-form-label>
              <nz-form-control>
                <nz-select id="type" formControlName="type">
                  <nz-option nzValue="PERCENT" nzLabel="Giảm theo %" />
                  <nz-option nzValue="AMOUNT" nzLabel="Giảm số tiền" />
                </nz-select>
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="value" nzRequired>Giá trị</nz-form-label>
              <nz-form-control nzErrorTip="Lớn hơn 0 (tối đa 100 nếu là %)">
                <nz-input-number id="value" formControlName="value" [nzMin]="1" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="maxDiscount">Giảm tối đa (₫, cho loại %)</nz-form-label>
              <nz-form-control>
                <nz-input-number id="maxDiscount" formControlName="maxDiscount" [nzMin]="1000" [nzStep]="10000" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="minOrder">Đơn tối thiểu (₫)</nz-form-label>
              <nz-form-control>
                <nz-input-number id="minOrder" formControlName="minOrder" [nzMin]="0" [nzStep]="10000" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="minTickets">Số vé tối thiểu (ưu đãi nhóm)</nz-form-label>
              <nz-form-control>
                <nz-input-number id="minTickets" formControlName="minTickets" [nzMin]="1" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="status">Trạng thái</nz-form-label>
              <nz-form-control>
                <nz-select id="status" formControlName="status">
                  <nz-option nzValue="ACTIVE" nzLabel="Đang bật" />
                  <nz-option nzValue="INACTIVE" nzLabel="Tắt" />
                </nz-select>
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="startsAt" nzRequired>Bắt đầu</nz-form-label>
              <nz-form-control nzErrorTip="Chọn thời điểm">
                <nz-date-picker id="startsAt" formControlName="startsAt" nzShowTime nzFormat="HH:mm dd/MM/yyyy" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="endsAt" nzRequired>Kết thúc</nz-form-label>
              <nz-form-control nzErrorTip="Chọn thời điểm">
                <nz-date-picker id="endsAt" formControlName="endsAt" nzShowTime nzFormat="HH:mm dd/MM/yyyy" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="usageLimit">Tổng lượt dùng</nz-form-label>
              <nz-form-control>
                <nz-input-number id="usageLimit" formControlName="usageLimit" [nzMin]="1" nzPlaceHolder="Không giới hạn" class="full" />
              </nz-form-control>
            </nz-form-item>
            <nz-form-item>
              <nz-form-label nzFor="perUserLimit">Lượt / người</nz-form-label>
              <nz-form-control>
                <nz-input-number id="perUserLimit" formControlName="perUserLimit" [nzMin]="1" nzPlaceHolder="Không giới hạn" class="full" />
              </nz-form-control>
            </nz-form-item>
          </div>
        </form>
      </ng-container>
    </nz-modal>
  `,
  styles: `
    .muted {
      color: rgba(0, 0, 0, 0.45);
      font-size: 12px;
    }

    .grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0 16px;
    }

    .full {
      width: 100%;
    }
  `,
})
export class PromotionsPage implements OnInit {
  protected readonly store = inject(PromotionsStore);
  private readonly fb = inject(FormBuilder);
  private readonly message = inject(NzMessageService);

  protected readonly vn = VN_OFFSET;
  protected readonly open = signal(false);
  protected editingId: number | null = null;

  protected readonly form = this.fb.group({
    code: this.fb.nonNullable.control('', [Validators.required, Validators.pattern(/^[A-Za-z0-9_-]{1,30}$/)]),
    name: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(120)]),
    type: this.fb.nonNullable.control<'PERCENT' | 'AMOUNT'>('PERCENT'),
    value: this.fb.control<number | null>(10, [Validators.required, Validators.min(1)]),
    maxDiscount: this.fb.control<number | null>(null),
    minOrder: this.fb.nonNullable.control(0),
    minTickets: this.fb.nonNullable.control(1),
    startsAt: this.fb.control<Date | null>(null, Validators.required),
    endsAt: this.fb.control<Date | null>(null, Validators.required),
    usageLimit: this.fb.control<number | null>(null),
    perUserLimit: this.fb.control<number | null>(null),
    status: this.fb.nonNullable.control<'ACTIVE' | 'INACTIVE'>('ACTIVE'),
  });

  ngOnInit(): void {
    this.store.load();
  }

  protected create(): void {
    this.editingId = null;
    const start = new Date();
    const end = new Date();
    end.setMonth(end.getMonth() + 1);
    this.form.reset({ type: 'PERCENT', value: 10, minOrder: 0, minTickets: 1, status: 'ACTIVE', startsAt: start, endsAt: end });
    this.open.set(true);
  }

  protected edit(p: Promotion): void {
    this.editingId = p.id;
    this.form.reset({
      ...p,
      startsAt: new Date(p.startsAt),
      endsAt: new Date(p.endsAt),
    });
    this.open.set(true);
  }

  protected submit(): void {
    const v = this.form.getRawValue();
    if (this.form.invalid || !v.startsAt || !v.endsAt || (v.type === 'PERCENT' && (v.value ?? 0) > 100)) {
      touchAll(this.form);
      return;
    }
    const request: PromotionRequest = {
      code: v.code.trim(),
      name: v.name.trim(),
      description: null,
      type: v.type,
      value: v.value ?? 0,
      maxDiscount: v.type === 'PERCENT' ? v.maxDiscount : null,
      minOrder: v.minOrder ?? 0,
      minTickets: v.minTickets ?? 1,
      startsAt: v.startsAt.toISOString(),
      endsAt: v.endsAt.toISOString(),
      usageLimit: v.usageLimit,
      perUserLimit: v.perUserLimit,
      status: v.status,
    };
    this.store.save(this.editingId, request).subscribe(() => {
      this.open.set(false);
      this.message.success('Đã lưu mã giảm giá');
    });
  }
}
