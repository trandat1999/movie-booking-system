import { Component, effect, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ACTIVE_STATUS_LABELS, ActiveStatus, Cinema, CinemaRequest, toOptions, touchAll } from '@mb/shared';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';

/** Form thêm/sửa rạp trong modal. {@link cinema} = undefined: đóng; null: thêm mới. */
@Component({
  selector: 'adm-cinema-form-modal',
  imports: [ReactiveFormsModule, NzModalModule, NzFormModule, NzInputModule, NzSelectModule],
  template: `
    <nz-modal
      [nzVisible]="cinema() !== undefined"
      [nzTitle]="cinema() ? 'Sửa rạp' : 'Thêm rạp'"
      nzOkText="Lưu"
      nzCancelText="Hủy"
      [nzOkLoading]="saving()"
      (nzOnCancel)="closed.emit()"
      (nzOnOk)="submit()"
    >
      <ng-container *nzModalContent>
        <form nz-form nzLayout="vertical" [formGroup]="form">
          <nz-form-item>
            <nz-form-label nzFor="name" nzRequired>Tên rạp</nz-form-label>
            <nz-form-control nzErrorTip="Vui lòng nhập tên rạp">
              <input nz-input id="name" formControlName="name" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="address" nzRequired>Địa chỉ</nz-form-label>
            <nz-form-control nzErrorTip="Vui lòng nhập địa chỉ">
              <input nz-input id="address" formControlName="address" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="city" nzRequired>Thành phố</nz-form-label>
            <nz-form-control nzErrorTip="Vui lòng nhập thành phố">
              <input nz-input id="city" formControlName="city" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="phone">Điện thoại</nz-form-label>
            <nz-form-control>
              <input nz-input id="phone" formControlName="phone" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="amenities">Tiện ích</nz-form-label>
            <nz-form-control>
              <nz-select id="amenities" nzMode="tags" formControlName="amenities" nzPlaceHolder="Gõ rồi nhấn Enter" />
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="description">Mô tả</nz-form-label>
            <nz-form-control>
              <textarea nz-input id="description" formControlName="description" rows="3"></textarea>
            </nz-form-control>
          </nz-form-item>
          <nz-form-item>
            <nz-form-label nzFor="status" nzRequired>Trạng thái</nz-form-label>
            <nz-form-control>
              <nz-select id="status" formControlName="status">
                @for (o of statusOptions; track o.value) {
                  <nz-option [nzValue]="o.value" [nzLabel]="o.label" />
                }
              </nz-select>
            </nz-form-control>
          </nz-form-item>
        </form>
      </ng-container>
    </nz-modal>
  `,
})
export class CinemaFormModalComponent {
  readonly cinema = input<Cinema | null | undefined>(undefined);
  readonly saving = input(false);
  readonly closed = output<void>();
  readonly saved = output<CinemaRequest>();

  protected readonly statusOptions = toOptions(ACTIVE_STATUS_LABELS);
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    address: ['', [Validators.required]],
    city: ['', [Validators.required]],
    phone: [''],
    description: [''],
    amenities: [[] as string[]],
    status: ['ACTIVE' as ActiveStatus],
  });

  constructor() {
    effect(() => {
      const c = this.cinema();
      if (c !== undefined) {
        this.form.reset({
          name: c?.name ?? '',
          address: c?.address ?? '',
          city: c?.city ?? '',
          phone: c?.phone ?? '',
          description: c?.description ?? '',
          amenities: c?.amenities ?? [],
          status: c?.status ?? 'ACTIVE',
        });
      }
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      touchAll(this.form);
      return;
    }
    const v = this.form.getRawValue();
    this.saved.emit({
      name: v.name.trim(),
      address: v.address.trim(),
      city: v.city.trim(),
      phone: v.phone.trim() || null,
      description: v.description.trim() || null,
      imageUrl: this.cinema()?.imageUrl ?? null,
      amenities: v.amenities,
      status: v.status,
    });
  }
}
