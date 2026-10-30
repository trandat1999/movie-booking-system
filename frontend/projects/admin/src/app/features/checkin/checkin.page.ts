import { DatePipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TICKET_STATUS_LABELS, VN_OFFSET } from '@mb/shared';
import { BrowserQRCodeReader, IScannerControls } from '@zxing/browser';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCardModule } from 'ng-zorro-antd/card';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';

import { CheckinStore } from './checkin.store';

/**
 * Trang soát vé cho nhân viên (CHK01): quét QR vé (một người) hoặc QR đơn (cả nhóm) bằng camera, hoặc dán nội
 * dung mã. Mỗi vé chỉ soát được một lần — quét lần hai hiện cảnh báo đỏ.
 */
@Component({
  selector: 'adm-checkin-page',
  providers: [CheckinStore],
  imports: [DatePipe, FormsModule, NzButtonModule, NzCardModule, NzInputModule, NzTableModule, NzTagModule],
  template: `
    <div class="layout">
      <nz-card nzTitle="Quét mã QR">
        <div class="camera">
          <video #video muted playsinline [class.hidden]="!scanning()"></video>
          @if (!scanning()) {
            <div class="placeholder">Camera đang tắt</div>
          }
        </div>
        <div class="actions">
          @if (scanning()) {
            <button nz-button (click)="stopCamera()">Tắt camera</button>
          } @else {
            <button nz-button nzType="primary" (click)="startCamera()">Bật camera</button>
          }
          @if (cameraError()) {
            <span class="error">{{ cameraError() }}</span>
          }
        </div>
        <form class="manual" (ngSubmit)="submitManual()">
          <label for="payload">Hoặc dán nội dung mã QR</label>
          <input id="payload" nz-input name="payload" [(ngModel)]="manual" placeholder="Ví dụ: TABC123….xyz" autocomplete="off" />
          <button nz-button type="submit" [nzLoading]="store.busy()" [disabled]="!manual.trim()">Soát vé</button>
        </form>
      </nz-card>

      <div class="result-col">
        @if (store.last(); as last) {
          <div class="result" [class.ok]="last.ok" [class.fail]="!last.ok" role="status">
            <div class="result-title">{{ last.ok ? '✔ ' : '✖ ' }}{{ last.message }}</div>
            @if (last.result; as r) {
              <div>{{ r.showtime.movieTitle }} · {{ r.showtime.startAt | date: 'HH:mm dd/MM' : vn }}</div>
              <div>{{ r.showtime.cinemaName }} · {{ r.showtime.roomName }}</div>
            }
          </div>
          @if (last.result; as r) {
            <nz-card nzTitle="Vé của đơn" nzSize="small">
              <nz-table [nzData]="r.tickets" [nzShowPagination]="false" nzSize="small" [nzFrontPagination]="false">
                <thead>
                  <tr>
                    <th>Ghế</th>
                    <th>Người giữ vé</th>
                    <th>Trạng thái</th>
                  </tr>
                </thead>
                <tbody>
                  @for (t of r.tickets; track t.code) {
                    <tr>
                      <td>{{ t.seatLabel }}</td>
                      <td>{{ t.holderName }}</td>
                      <td>
                        <nz-tag [nzColor]="t.status === 'USED' ? 'green' : t.status === 'VALID' ? 'blue' : 'default'">
                          {{ ticketLabels[t.status] }}
                        </nz-tag>
                        @if (t.checkedInAt) {
                          <span class="muted">{{ t.checkedInAt | date: 'HH:mm' : vn }}</span>
                        }
                      </td>
                    </tr>
                  }
                </tbody>
              </nz-table>
            </nz-card>
          }
        } @else {
          <div class="result idle">Đưa mã QR vào khung camera hoặc dán nội dung mã để soát vé.</div>
        }

        @if (store.history().length > 1) {
          <nz-card nzTitle="Lượt quét gần đây" nzSize="small" class="history">
            @for (h of store.history(); track h.at) {
              <div class="history-item" [class.fail]="!h.ok">
                {{ h.at | date: 'HH:mm:ss' }} — {{ h.message }}
              </div>
            }
          </nz-card>
        }
      </div>
    </div>
  `,
  styles: `
    .layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 16px;
      align-items: start;
    }

    .camera {
      position: relative;
      aspect-ratio: 4 / 3;
      background: #141414;
      border-radius: 8px;
      overflow: hidden;

      video {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }

    .hidden {
      display: none;
    }

    .placeholder {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: #8c8c8c;
    }

    .actions {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 12px 0;
    }

    .manual {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 8px;

      label {
        width: 100%;
      }

      input {
        flex: 1;
        min-width: 200px;
      }
    }

    .result {
      margin-bottom: 16px;
      padding: 20px;
      border-radius: 8px;
      font-size: 15px;
    }

    .result-title {
      margin-bottom: 4px;
      font-size: 20px;
      font-weight: 600;
    }

    .ok {
      background: #f6ffed;
      border: 2px solid #52c41a;
      color: #135200;
    }

    .fail {
      background: #fff1f0;
      border: 2px solid #ff4d4f;
      color: #a8071a;
    }

    .idle {
      background: #fafafa;
      border: 2px dashed #d9d9d9;
      color: #8c8c8c;
    }

    .error {
      color: #cf1322;
    }

    .muted {
      color: rgba(0, 0, 0, 0.45);
      margin-left: 6px;
    }

    .history {
      margin-top: 16px;
    }

    .history-item {
      padding: 2px 0;

      &.fail {
        color: #cf1322;
      }
    }

    @media (max-width: 900px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class CheckinPage {
  protected readonly store = inject(CheckinStore);

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');

  protected readonly vn = VN_OFFSET;
  protected readonly ticketLabels = TICKET_STATUS_LABELS;
  protected readonly scanning = signal(false);
  protected readonly cameraError = signal<string | null>(null);
  protected manual = '';

  private controls: IScannerControls | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.stopCamera());
  }

  protected async startCamera(): Promise<void> {
    this.cameraError.set(null);
    try {
      const reader = new BrowserQRCodeReader();
      this.scanning.set(true);
      this.controls = await reader.decodeFromVideoDevice(undefined, this.video().nativeElement, (result) => {
        if (result) {
          this.store.scan(result.getText());
        }
      });
    } catch {
      this.scanning.set(false);
      this.cameraError.set('Không mở được camera — kiểm tra quyền truy cập camera của trình duyệt.');
    }
  }

  protected stopCamera(): void {
    this.controls?.stop();
    this.controls = null;
    this.scanning.set(false);
  }

  protected submitManual(): void {
    this.store.scan(this.manual);
    this.manual = '';
  }
}
