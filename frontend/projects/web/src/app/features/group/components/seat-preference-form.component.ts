import { Component, computed, effect, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { GroupMemberView, SEAT_PREFERENCE_LABELS, SeatPreference, SeatPreferenceRequest } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';

/** Sở thích chỗ ngồi (GRP22): dùng khi hệ thống phân phối vé trong cụm ghế đã mua. */
@Component({
  selector: 'app-seat-preference-form',
  imports: [FormsModule, NzButtonModule, NzSelectModule],
  template: `
    <form class="form" (ngSubmit)="submit()">
      <label for="preference">Vị trí ưa thích</label>
      <nz-select id="preference" name="preference" [(ngModel)]="preference">
        @for (p of preferences; track p) {
          <nz-option [nzValue]="p" [nzLabel]="labels[p]" />
        }
      </nz-select>
      <label for="nextTo">Muốn ngồi cạnh</label>
      <nz-select id="nextTo" name="nextTo" [(ngModel)]="nextTo" nzAllowClear nzPlaceHolder="Không yêu cầu">
        @for (m of others(); track m.userId) {
          <nz-option [nzValue]="m.userId" [nzLabel]="m.fullName" />
        }
      </nz-select>
      <div class="actions">
        <button nz-button type="submit" [nzLoading]="saving()">Lưu sở thích</button>
        <span class="hint">Khách đi kèm luôn được xếp cạnh bạn.</span>
      </div>
    </form>
  `,
  styles: `
    .form {
      display: grid;
      grid-template-columns: auto minmax(0, 240px);
      align-items: center;
      gap: 8px 12px;
    }

    .actions {
      grid-column: 1 / -1;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 12px;
    }

    .hint {
      color: rgba(0, 0, 0, 0.45);
      font-size: 13px;
    }

    @media (max-width: 480px) {
      .form {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class SeatPreferenceFormComponent {
  readonly member = input.required<GroupMemberView>();
  readonly members = input.required<GroupMemberView[]>();
  readonly saving = input(false);
  readonly saved = output<SeatPreferenceRequest>();

  protected readonly preferences: SeatPreference[] = ['NONE', 'AISLE', 'CENTER'];
  protected readonly labels = SEAT_PREFERENCE_LABELS;
  protected readonly others = computed(() =>
    this.members().filter((m) => m.userId !== this.member().userId && m.rsvp !== 'NO'),
  );

  protected preference: SeatPreference = 'NONE';
  protected nextTo: number | null = null;

  constructor() {
    effect(() => {
      const m = this.member();
      this.preference = m.seatPreference;
      this.nextTo = m.nextToUserId;
    });
  }

  protected submit(): void {
    this.saved.emit({ preference: this.preference, nextToUserId: this.nextTo ?? null });
  }
}
