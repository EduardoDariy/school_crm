import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { LOGIN_FAILURE_LABELS, ROLE_LABELS, ROLES } from '../../core/labels';
import { LoginAttempt, StaffRole, User } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { ToastService, errorMessage } from '../../core/toast.service';
import { ModalComponent } from '../../shared/modal.component';

type Dialog = { kind: 'create' } | { kind: 'edit'; user: User } | { kind: 'password'; user: User };

/** Сотрудники школы и журнал входов — для администратора. */
@Component({
  selector: 'app-staff',
  imports: [FormsModule, DatePipe, ModalComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <h1>Сотрудники</h1>
        <span class="spacer"></span>
        @if (tab() === 'users') { <button class="btn btn-primary" (click)="openCreate()">+ Сотрудник</button> }
      </div>
      <div class="tabs">
        <button [class.active]="tab() === 'users'" (click)="tab.set('users')">Сотрудники</button>
        <button [class.active]="tab() === 'journal'" (click)="openJournal()">Журнал входов</button>
      </div>

      @if (tab() === 'users') {
        <div class="card">
          <table class="table">
            <tr><th>Сотрудник</th><th>Email</th><th>Роль</th><th>Последний вход</th><th>Статус</th><th></th></tr>
            @for (user of users(); track user.id) {
              <tr [class.blocked]="user.blocked">
                <td>{{ user.fullName }}@if (user.id === auth.user()?.id) { <span class="muted"> (вы)</span> }</td>
                <td class="muted">{{ user.email }}</td>
                <td>{{ roleLabels[user.role] }}</td>
                <td class="muted">{{ user.lastLoginAt ? (user.lastLoginAt | date: 'd MMM, HH:mm') : 'не входил' }}</td>
                <td>
                  @if (user.blocked) { <span class="badge badge-danger">Заблокирован</span> }
                  @else { <span class="badge badge-success">Активен</span> }
                </td>
                <td class="actions">
                  <button class="btn btn-sm" (click)="openEdit(user)">Изменить</button>
                  <button class="btn btn-sm" (click)="openPassword(user)">Пароль</button>
                  @if (user.id !== auth.user()?.id) {
                    <button class="btn btn-sm" [class.btn-danger]="!user.blocked" (click)="toggleBlocked(user)">
                      {{ user.blocked ? 'Разблокировать' : 'Заблокировать' }}
                    </button>
                  }
                </td>
              </tr>
            }
          </table>
        </div>
      } @else {
        <div class="card">
          @if (journal().length === 0) { <div class="empty">Записей нет</div> }
          <table class="table">
            @if (journal().length > 0) { <tr><th>Время</th><th>Email</th><th>Результат</th><th>IP</th><th>Браузер</th></tr> }
            @for (a of journal(); track a.id) {
              <tr>
                <td>{{ a.createdAt | date: 'd MMM y, HH:mm:ss' }}</td>
                <td>{{ a.email }}</td>
                <td>
                  @if (a.success) { <span class="badge badge-success">Успешно</span> }
                  @else { <span class="badge badge-danger">{{ failureLabel(a.failureReason) }}</span> }
                </td>
                <td class="muted">{{ a.ipAddress }}</td>
                <td class="muted small ua" [title]="a.userAgent ?? ''">{{ a.userAgent }}</td>
              </tr>
            }
          </table>
        </div>
      }
    </div>

    @if (dialog(); as d) {
      <app-modal [title]="d.kind === 'create' ? 'Новый сотрудник' : d.kind === 'edit' ? 'Изменить сотрудника' : 'Новый пароль'"
                 (closed)="dialog.set(null)">
        <form id="staff-form" class="stack" (ngSubmit)="save(d)">
          @if (d.kind !== 'password') {
            <label class="field"><span>ФИО</span><input class="input" name="fullName" [(ngModel)]="form.fullName" required /></label>
            @if (d.kind === 'create') {
              <label class="field"><span>Email (логин)</span><input class="input" type="email" name="email" [(ngModel)]="form.email" required /></label>
            }
            <label class="field"><span>Роль</span>
              <select class="input" name="role" [(ngModel)]="form.role">
                @for (r of roles; track r) { <option [value]="r">{{ roleLabels[r] }}</option> }
              </select>
            </label>
          } @else {
            <div class="muted">Сотрудник: {{ d.user.fullName }}</div>
          }
          @if (d.kind !== 'edit') {
            <label class="field"><span>Пароль (не короче 8 символов)</span>
              <input class="input" type="text" name="password" [(ngModel)]="form.password" required minlength="8" autocomplete="new-password" />
            </label>
            <div class="muted small">Передайте пароль сотруднику — он сможет сменить его после входа.</div>
          }
          @if (formError) { <div class="error-text">{{ formError }}</div> }
        </form>
        <ng-container actions>
          <button class="btn" type="button" (click)="dialog.set(null)">Отмена</button>
          <button class="btn btn-primary" type="submit" form="staff-form">Сохранить</button>
        </ng-container>
      </app-modal>
    }
  `,
  styles: `
    .spacer { flex: 1; }
    .actions { text-align: right; white-space: nowrap; }
    .actions .btn { margin-left: 6px; }
    tr.blocked td:first-child { color: var(--text-muted); }
    .ua { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  `,
})
export class StaffComponent implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  private readonly directory = inject(StaffDirectoryService);
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roles = ROLES;

  protected readonly tab = signal<'users' | 'journal'>('users');
  protected readonly users = signal<User[]>([]);
  protected readonly journal = signal<LoginAttempt[]>([]);
  protected readonly dialog = signal<Dialog | null>(null);

  form: { fullName: string; email: string; role: StaffRole; password: string } =
    { fullName: '', email: '', role: 'MANAGER', password: '' };
  formError = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.api.users().subscribe({ next: u => this.users.set(u), error: e => this.toast.error(e) });
  }

  openJournal(): void {
    this.tab.set('journal');
    this.api.loginJournal().subscribe({ next: p => this.journal.set(p.content), error: e => this.toast.error(e) });
  }

  openCreate(): void {
    this.form = { fullName: '', email: '', role: 'MANAGER', password: '' };
    this.formError = '';
    this.dialog.set({ kind: 'create' });
  }

  openEdit(user: User): void {
    this.form = { fullName: user.fullName, email: user.email, role: user.role, password: '' };
    this.formError = '';
    this.dialog.set({ kind: 'edit', user });
  }

  openPassword(user: User): void {
    this.form = { ...this.form, password: '' };
    this.formError = '';
    this.dialog.set({ kind: 'password', user });
  }

  save(d: Dialog): void {
    const request = d.kind === 'create'
      ? this.api.createUser({ email: this.form.email, fullName: this.form.fullName, role: this.form.role, password: this.form.password })
      : d.kind === 'edit'
        ? this.api.updateUser(d.user.id, { fullName: this.form.fullName, role: this.form.role })
        : this.api.setUserPassword(d.user.id, this.form.password);
    (request as import('rxjs').Observable<unknown>).subscribe({
      next: () => {
        this.toast.success(d.kind === 'password' ? 'Пароль изменён' : 'Сохранено');
        this.dialog.set(null);
        this.load();
        this.directory.load(true);
      },
      error: e => (this.formError = errorMessage(e)),
    });
  }

  toggleBlocked(user: User): void {
    if (!user.blocked && !confirm(`Заблокировать ${user.fullName}? Сотрудник сразу потеряет доступ, история сохранится.`)) {
      return;
    }
    this.api.setUserBlocked(user.id, !user.blocked).subscribe({
      next: () => {
        this.toast.success(user.blocked ? 'Сотрудник разблокирован' : 'Сотрудник заблокирован');
        this.load();
        this.directory.load(true);
      },
      error: e => this.toast.error(e),
    });
  }

  failureLabel(reason: string | null): string {
    return reason ? LOGIN_FAILURE_LABELS[reason] ?? reason : 'Ошибка';
  }
}
