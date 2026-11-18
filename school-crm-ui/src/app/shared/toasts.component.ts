import { Component, inject } from '@angular/core';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <div class="toasts" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div class="toast" [class.error]="toast.kind === 'error'" (click)="toasts.dismiss(toast.id)">
          {{ toast.text }}
        </div>
      }
    </div>
  `,
  styles: `
    .toasts { position: fixed; right: 20px; bottom: 20px; display: flex; flex-direction: column; gap: 8px; z-index: 200; }
    .toast { background: #1d2330; color: #fff; padding: 10px 14px; border-radius: 8px; box-shadow: var(--shadow-lg);
      max-width: 380px; cursor: pointer; border-left: 4px solid var(--success); }
    .toast.error { border-left-color: #ff6b6b; }
  `,
})
export class ToastsComponent {
  readonly toasts = inject(ToastService);
}
