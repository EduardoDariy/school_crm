import { Component, inject, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { LEAD_SOURCES } from '../../core/labels';
import { Lead } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { ToastService, duplicateId, errorMessage } from '../../core/toast.service';
import { ModalComponent } from '../../shared/modal.component';

/** Новый лид. При дубле по телефону/мессенджеру предлагает открыть существующую карточку. */
@Component({
  selector: 'app-lead-create-dialog',
  imports: [FormsModule, RouterLink, ModalComponent],
  template: `
    <app-modal title="Новый лид" [width]="520" (closed)="closed.emit()">
      <form id="lead-form" class="form-grid" (ngSubmit)="save()">
        <label class="field full"><span>Имя</span>
          <input class="input" name="name" [(ngModel)]="name" required placeholder="Как обращаться к клиенту" />
        </label>
        <label class="field"><span>Телефон</span>
          <input class="input" name="phone" [(ngModel)]="phone" placeholder="+7 701 000 00 00" />
        </label>
        <label class="field"><span>Мессенджер (ник, id)</span>
          <input class="input" name="messenger" [(ngModel)]="messengerId" placeholder="@username" />
        </label>
        <label class="field"><span>Источник</span>
          <select class="input" name="source" [(ngModel)]="source">
            <option value="">—</option>
            @for (s of sources; track s) { <option [value]="s">{{ s }}</option> }
          </select>
        </label>
        <label class="field"><span>Ответственный</span>
          <select class="input" name="manager" [(ngModel)]="managerId">
            <option value="">{{ auth.hasRole('MANAGER') ? 'Я' : 'Не назначен' }}</option>
            @for (s of staff.leadOwners(); track s.id) {
              @if (s.id !== auth.user()?.id) { <option [value]="s.id">{{ s.fullName }}</option> }
            }
          </select>
        </label>
        @if (error) {
          <div class="full error-text">
            {{ error }}
            @if (existingId) { — <a [routerLink]="['/leads', existingId]" (click)="closed.emit()">открыть карточку</a> }
          </div>
        }
      </form>
      <ng-container actions>
        <button class="btn" type="button" (click)="closed.emit()">Отмена</button>
        <button class="btn btn-primary" type="submit" form="lead-form" [disabled]="saving || !name.trim()">Создать</button>
      </ng-container>
    </app-modal>
  `,
})
export class LeadCreateDialogComponent {
  readonly closed = output<void>();
  readonly created = output<Lead>();

  protected readonly auth = inject(AuthService);
  protected readonly staff = inject(StaffDirectoryService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  protected readonly sources = LEAD_SOURCES;

  name = '';
  phone = '';
  messengerId = '';
  source = '';
  managerId = '';
  saving = false;
  error = '';
  existingId: string | null = null;

  save(): void {
    this.saving = true;
    this.error = '';
    this.existingId = null;
    this.api.createLead({
      name: this.name.trim(),
      phoneNumber: this.phone.trim() || null,
      messengerId: this.messengerId.trim() || null,
      source: this.source || null,
      managerId: this.managerId || null,
    }).subscribe({
      next: lead => {
        this.toast.success('Лид создан');
        this.created.emit(lead);
      },
      error: e => {
        this.saving = false;
        this.error = errorMessage(e);
        this.existingId = duplicateId(e);
      },
    });
  }
}
