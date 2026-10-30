import { Component, effect, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GroupMemberView, Rsvp, RsvpRequest } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzRadioModule } from 'ng-zorro-antd/radio';

/** Phản hồi tham gia + khách đi kèm + ngân sách (GRP05). */
@Component({
  selector: 'app-rsvp-form',
  imports: [FormsModule, NzButtonModule, NzInputNumberModule, NzRadioModule],
  template: `
    <form class="form" (ngSubmit)="submit()">
      <!-- eslint-disable @angular-eslint/template/label-has-associated-control -->
      <nz-radio-group name="rsvp" [(ngModel)]="rsvp" nzButtonStyle="solid" [nzDisabled]="disabled()">
        <label nz-radio-button nzValue="YES">Tham gia</label>
        <label nz-radio-button nzValue="MAYBE">Chưa chắc</label>
        <label nz-radio-button nzValue="NO">Không tham gia</label>
      </nz-radio-group>
      <!-- eslint-enable @angular-eslint/template/label-has-associated-control -->
      @if (rsvp !== 'NO') {
        <div class="fields">
          <label for="guests">Khách đi kèm (không cần tài khoản)</label>
          <nz-input-number id="guests" name="guests" [(ngModel)]="guests" [nzMin]="0" [nzMax]="19" [nzDisabled]="disabled()" />
          <label for="budget">Ngân sách tối đa mỗi người (₫)</label>
          <nz-input-number
            id="budget"
            name="budget"
            [(ngModel)]="budget"
            [nzMin]="10000"
            [nzStep]="10000"
            nzPlaceHolder="Không giới hạn"
            [nzDisabled]="disabled()"
          />
        </div>
      }
      <button nz-button nzType="primary" type="submit" [disabled]="disabled() || rsvp === 'PENDING'" [nzLoading]="saving()">
        Lưu phản hồi
      </button>
    </form>
  `,
  styles: `
    .form {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .fields {
      display: grid;
      grid-template-columns: auto 160px;
      align-items: center;
      gap: 8px 12px;
    }

    @media (max-width: 480px) {
      .fields {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class RsvpFormComponent {
  readonly member = input.required<GroupMemberView>();
  readonly disabled = input(false);
  readonly saving = input(false);
  readonly saved = output<RsvpRequest>();

  protected rsvp: Rsvp = 'PENDING';
  protected guests = 0;
  protected budget: number | null = null;

  constructor() {
    effect(() => {
      const m = this.member();
      this.rsvp = m.rsvp;
      this.guests = m.guestCount;
      this.budget = m.maxBudget;
    });
  }

  protected submit(): void {
    this.saved.emit({
      rsvp: this.rsvp,
      guestCount: this.rsvp === 'NO' ? 0 : this.guests ?? 0,
      maxBudget: this.rsvp === 'NO' ? null : this.budget,
    });
  }
}
