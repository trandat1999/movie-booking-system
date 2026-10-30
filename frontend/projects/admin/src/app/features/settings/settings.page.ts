import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Setting } from '@mb/shared';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTableModule } from 'ng-zorro-antd/table';

import { SettingsStore } from './settings.store';

/** Tham số nghiệp vụ (SYS01) — nơi duy nhất cấu hình thời gian giữ ghế, quy tắc nhóm, hủy vé... */
@Component({
  selector: 'adm-settings-page',
  providers: [SettingsStore],
  imports: [DatePipe, FormsModule, NzCardModule, NzTableModule, NzInputNumberModule, NzSelectModule, NzButtonModule],
  template: `
    @for (group of store.groups(); track group.name) {
      <nz-card [nzTitle]="group.name">
        <nz-table [nzData]="group.items" [nzShowPagination]="false" nzSize="middle" [nzScroll]="{ x: '640px' }">
          <thead>
            <tr>
              <th>Tham số</th>
              <th nzWidth="200px">Giá trị</th>
              <th nzWidth="160px">Cập nhật</th>
              <th nzWidth="80px"></th>
            </tr>
          </thead>
          <tbody>
            @for (s of group.items; track s.key) {
              <tr>
                <td>
                  <div>{{ s.description }}</div>
                  <code class="key">{{ s.key }}</code>
                </td>
                <td>
                  @if (s.type === 'OPTION') {
                    <nz-select [(ngModel)]="draft[s.key]" [attr.aria-label]="s.description" class="value">
                      @for (o of s.options ?? []; track o) {
                        <nz-option [nzValue]="o" [nzLabel]="o" />
                      }
                    </nz-select>
                  } @else {
                    <nz-input-number
                      [(ngModel)]="draft[s.key]"
                      [nzMin]="s.min ?? 0"
                      [nzMax]="s.max ?? 1000000"
                      [attr.aria-label]="s.description"
                      class="value"
                    />
                    <div class="range">{{ s.min }} – {{ s.max }}</div>
                  }
                </td>
                <td class="range">{{ s.updatedAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                <td>
                  <button nz-button nzSize="small" nzType="primary" [disabled]="String(draft[s.key]) === s.value" (click)="save(s)">
                    Lưu
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </nz-table>
      </nz-card>
    }
  `,
  styles: `
    nz-card + nz-card {
      margin-top: 16px;
    }

    .key,
    .range {
      color: rgba(0, 0, 0, 0.45);
      font-size: 12px;
    }

    .value {
      width: 160px;
    }
  `,
})
export class SettingsPage implements OnInit {
  protected readonly store = inject(SettingsStore);
  private readonly message = inject(NzMessageService);

  protected readonly String = String;
  protected draft: Record<string, string | number> = {};

  ngOnInit(): void {
    this.store.load().subscribe((settings) => {
      this.draft = Object.fromEntries(settings.map((s) => [s.key, s.type === 'INTEGER' ? Number(s.value) : s.value]));
    });
  }

  protected save(setting: Setting): void {
    // Lỗi validate được interceptor hiển thị
    this.store.save(setting.key, String(this.draft[setting.key])).subscribe(() => this.message.success('Đã lưu cấu hình'));
  }
}
