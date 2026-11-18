import { Component, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { Task } from '../core/models';
import { StaffDirectoryService } from '../core/staff-directory.service';
import { ToastService } from '../core/toast.service';
import { defaultDueAt, toApiDateTime } from './date-utils';
import { ModalComponent } from './modal.component';

/** Новая задача — себе или коллеге, при необходимости по лиду. */
@Component({
  selector: 'app-task-dialog',
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Новая задача" (closed)="closed.emit()">
      <form id="task-form" class="stack" (ngSubmit)="save()">
        <label class="field"><span>Что сделать</span>
          <input class="input" name="title" [(ngModel)]="title" required placeholder="Например, перезвонить и пригласить на пробное" />
        </label>
        <div class="form-grid">
          <label class="field"><span>Срок</span>
            <input class="input" type="datetime-local" name="dueAt" [(ngModel)]="dueAt" required />
          </label>
          <label class="field"><span>Исполнитель</span>
            <select class="input" name="assignee" [(ngModel)]="assigneeId">
              @for (s of staff.active(); track s.id) {
                <option [value]="s.id">{{ s.fullName }}{{ s.id === auth.user()?.id ? ' (я)' : '' }}</option>
              }
            </select>
          </label>
        </div>
        <label class="field"><span>Комментарий</span>
          <textarea class="input" name="description" [(ngModel)]="description"></textarea>
        </label>
      </form>
      <ng-container actions>
        <button class="btn" type="button" (click)="closed.emit()">Отмена</button>
        <button class="btn btn-primary" type="submit" form="task-form" [disabled]="saving || !title.trim()">Создать</button>
      </ng-container>
    </app-modal>
  `,
})
export class TaskDialogComponent {
  readonly leadId = input<string | null>(null);
  readonly closed = output<void>();
  readonly created = output<Task>();

  protected readonly auth = inject(AuthService);
  protected readonly staff = inject(StaffDirectoryService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);

  title = '';
  description = '';
  dueAt = defaultDueAt();
  assigneeId = this.auth.user()?.id ?? '';
  saving = false;

  constructor() {
    this.staff.load();
  }

  save(): void {
    if (!this.title.trim()) {
      return;
    }
    this.saving = true;
    this.api.createTask({
      title: this.title.trim(),
      description: this.description.trim() || null,
      dueAt: toApiDateTime(this.dueAt),
      assigneeId: this.assigneeId || null,
      leadId: this.leadId(),
    }).subscribe({
      next: task => {
        this.toast.success('Задача создана');
        this.created.emit(task);
      },
      error: e => {
        this.saving = false;
        this.toast.error(e);
      },
    });
  }
}
