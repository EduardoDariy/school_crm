import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { LEAD_EVENT_LABELS, LEAD_SOURCES, LEAD_STATUS_LABELS } from '../../core/labels';
import { Lead, LeadEvent, Pipeline, Task } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { ToastService, duplicateId, errorMessage } from '../../core/toast.service';
import { ModalComponent } from '../../shared/modal.component';
import { TaskDialogComponent } from '../../shared/task-dialog.component';
import { TaskListComponent } from '../../shared/task-list.component';

@Component({
  selector: 'app-lead-detail',
  imports: [FormsModule, RouterLink, DatePipe, ModalComponent, TaskListComponent, TaskDialogComponent],
  template: `
    <div class="page">
      <a routerLink="/leads" class="small">← Все лиды</a>
      @if (lead(); as lead) {
        <div class="page-header head">
          <h1>{{ lead.name }}</h1>
          <span class="badge" [class.badge-accent]="lead.status === 'OPEN'" [class.badge-success]="lead.status === 'WON'"
                [class.badge-danger]="lead.status === 'LOST'">{{ statusLabels[lead.status] }}</span>
          @if (lead.archived) { <span class="badge badge-warning">В архиве</span> }
          <span class="spacer"></span>
          @if (!lead.archived) {
            @if (lead.status === 'OPEN') {
              <button class="btn btn-success" (click)="win()">Успешно — в ученики</button>
              <button class="btn btn-danger" (click)="loseOpen.set(true)">Проигран</button>
            } @else {
              <button class="btn" (click)="reopen()">Вернуть в работу</button>
            }
            <button class="btn" (click)="setArchived(true)">В архив</button>
          } @else {
            <button class="btn" (click)="setArchived(false)">Вернуть из архива</button>
          }
        </div>

        @if (lead.status === 'OPEN' && !lead.archived) {
          <div class="stages">
            @for (stage of pipeline()?.stages ?? []; track stage.id; let i = $index) {
              <button class="stage" [class.current]="stage.id === lead.stageId" [class.passed]="i < currentStageIndex()"
                      (click)="moveTo(stage.id)">{{ stage.name }}</button>
            }
          </div>
        }

        <div class="layout">
          <div class="stack">
            <section class="card">
              <div class="row section-head">
                <h2>Контакты</h2><span class="spacer"></span>
                @if (!editing() && !lead.archived) { <button class="btn btn-sm" (click)="startEdit(lead)">Изменить</button> }
              </div>
              @if (editing()) {
                <form class="form-grid" (ngSubmit)="saveEdit()">
                  <label class="field full"><span>Имя</span><input class="input" name="name" [(ngModel)]="form.name" required /></label>
                  <label class="field"><span>Телефон</span><input class="input" name="phone" [(ngModel)]="form.phoneNumber" /></label>
                  <label class="field"><span>Мессенджер</span><input class="input" name="messenger" [(ngModel)]="form.messengerId" /></label>
                  <label class="field"><span>Источник</span>
                    <select class="input" name="source" [(ngModel)]="form.source">
                      <option value="">—</option>
                      @for (s of sources; track s) { <option [value]="s">{{ s }}</option> }
                    </select>
                  </label>
                  @if (editError) {
                    <div class="full error-text">{{ editError }}
                      @if (editDuplicateId) { — <a [routerLink]="['/leads', editDuplicateId]">открыть</a> }
                    </div>
                  }
                  <div class="full row">
                    <button class="btn btn-primary" type="submit">Сохранить</button>
                    <button class="btn" type="button" (click)="editing.set(false)">Отмена</button>
                  </div>
                </form>
              } @else {
                <dl class="props">
                  <dt>Телефон</dt><dd>{{ lead.phoneNumber || '—' }}</dd>
                  <dt>Мессенджер</dt><dd>{{ lead.messengerId || '—' }}</dd>
                  <dt>Источник</dt><dd>{{ lead.source || '—' }}</dd>
                  <dt>Ответственный</dt>
                  <dd>
                    <select class="input inline" [ngModel]="lead.managerId ?? ''" (ngModelChange)="assign($event)" [disabled]="lead.archived">
                      <option value="">Не назначен</option>
                      @for (s of staff.leadOwners(); track s.id) { <option [value]="s.id">{{ s.fullName }}</option> }
                    </select>
                  </dd>
                  <dt>Создан</dt><dd>{{ lead.createdAt | date: 'd MMMM y, HH:mm' }}</dd>
                  @if (lead.closedAt) { <dt>Закрыт</dt><dd>{{ lead.closedAt | date: 'd MMMM y, HH:mm' }}</dd> }
                  @if (lead.status === 'LOST') {
                    <dt>Причина</dt><dd>{{ reasonName(lead.lostReasonId) }}{{ lead.lostComment ? ' — ' + lead.lostComment : '' }}</dd>
                  }
                  @if (lead.personId) { <dt>Ученик</dt><dd><span class="badge badge-success">Карточка создана</span></dd> }
                </dl>
              }
            </section>

            <section class="card">
              <div class="row section-head">
                <h2>Задачи</h2><span class="spacer"></span>
                <button class="btn btn-sm" (click)="taskOpen.set(true)">+ Задача</button>
              </div>
              <app-task-list [tasks]="tasks()" [showAssignee]="true" [showLead]="false" emptyText="Задач по лиду нет"
                             (changed)="loadTasks()" />
            </section>
          </div>

          <section class="card">
            <h2>История</h2>
            <ol class="timeline">
              @for (event of history(); track event.id) {
                <li>
                  <div class="event-title">{{ eventLabels[event.type] }}</div>
                  <div class="small">{{ describe(event) }}</div>
                  <div class="muted small">{{ event.createdAt | date: 'd MMM, HH:mm' }} · {{ staff.name(event.actorId) }}</div>
                </li>
              }
            </ol>
          </section>
        </div>
      } @else if (notFound()) {
        <div class="empty">Лид не найден</div>
      }
    </div>

    @if (loseOpen()) {
      <app-modal title="Лид проигран" [width]="420" (closed)="loseOpen.set(false)">
        <div class="stack">
          @if (pipeline()?.lostReasonMode !== 'DISABLED') {
            <label class="field">
              <span>Причина{{ pipeline()?.lostReasonMode === 'REQUIRED' ? ' (обязательно)' : '' }}</span>
              <select class="input" [(ngModel)]="loseReasonId">
                <option value="">—</option>
                @for (r of activeReasons(); track r.id) { <option [value]="r.id">{{ r.name }}</option> }
              </select>
            </label>
          }
          <label class="field"><span>Комментарий</span><textarea class="input" [(ngModel)]="loseComment"></textarea></label>
        </div>
        <ng-container actions>
          <button class="btn" (click)="loseOpen.set(false)">Отмена</button>
          <button class="btn btn-primary" (click)="lose()"
                  [disabled]="pipeline()?.lostReasonMode === 'REQUIRED' && !loseReasonId">Сохранить</button>
        </ng-container>
      </app-modal>
    }

    @if (taskOpen()) {
      <app-task-dialog [leadId]="id()" (closed)="taskOpen.set(false)" (created)="taskOpen.set(false); loadTasks()" />
    }
  `,
  styles: `
    .head { margin-top: 8px; }
    .spacer { flex: 1; }
    .stages { display: flex; margin-bottom: 20px; border-radius: var(--radius); overflow: hidden; border: 1px solid var(--border); }
    .stage { flex: 1; border: none; border-right: 1px solid var(--border); background: var(--surface); padding: 9px 8px;
      font: inherit; cursor: pointer; color: var(--text-muted); }
    .stage:last-child { border-right: none; }
    .stage:hover { background: var(--surface-muted); }
    .stage.passed { background: var(--accent-soft); color: var(--accent); }
    .stage.current { background: var(--accent); color: #fff; font-weight: 500; }
    .layout { display: grid; grid-template-columns: minmax(0, 3fr) minmax(280px, 2fr); gap: 16px; align-items: start; }
    .section-head { margin-bottom: 8px; }
    .section-head h2 { margin: 0; }
    .props { display: grid; grid-template-columns: 140px 1fr; gap: 10px 12px; margin: 0; align-items: center; }
    .props dt { color: var(--text-muted); }
    .props dd { margin: 0; }
    .inline { max-width: 260px; }
    .timeline { list-style: none; margin: 0; padding: 0 0 0 14px; border-left: 2px solid var(--border); }
    .timeline li { position: relative; padding: 0 0 14px 10px; }
    .timeline li::before { content: ''; position: absolute; left: -21px; top: 4px; width: 10px; height: 10px;
      border-radius: 50%; background: var(--accent); border: 2px solid var(--surface); }
    .event-title { font-weight: 500; }
    @media (max-width: 1000px) { .layout { grid-template-columns: 1fr; } }
  `,
})
export class LeadDetailComponent implements OnInit {
  /** id из маршрута /leads/:id. */
  readonly id = input.required<string>();

  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  protected readonly staff = inject(StaffDirectoryService);
  protected readonly statusLabels = LEAD_STATUS_LABELS;
  protected readonly eventLabels = LEAD_EVENT_LABELS;
  protected readonly sources = LEAD_SOURCES;

  protected readonly lead = signal<Lead | null>(null);
  protected readonly notFound = signal(false);
  protected readonly pipelines = signal<Pipeline[]>([]);
  protected readonly history = signal<LeadEvent[]>([]);
  protected readonly tasks = signal<Task[]>([]);
  protected readonly editing = signal(false);
  protected readonly loseOpen = signal(false);
  protected readonly taskOpen = signal(false);

  protected readonly pipeline = computed(() => this.pipelines().find(p => p.id === this.lead()?.pipelineId) ?? null);
  protected readonly currentStageIndex = computed(() =>
    (this.pipeline()?.stages ?? []).findIndex(s => s.id === this.lead()?.stageId));
  protected readonly activeReasons = computed(() => (this.pipeline()?.lostReasons ?? []).filter(r => r.active));

  form = { name: '', phoneNumber: '', messengerId: '', source: '' };
  editError = '';
  editDuplicateId: string | null = null;
  loseReasonId = '';
  loseComment = '';

  ngOnInit(): void {
    this.staff.load();
    this.api.pipelines().subscribe(p => this.pipelines.set(p));
    this.api.lead(this.id()).subscribe({
      next: lead => this.lead.set(lead),
      error: () => this.notFound.set(true),
    });
    this.loadHistory();
    this.loadTasks();
  }

  loadHistory(): void {
    this.api.leadHistory(this.id()).subscribe(h => this.history.set(h.slice().reverse()));
  }

  loadTasks(): void {
    this.api.tasks({ leadId: this.id(), anyStatus: true }).subscribe(t => this.tasks.set(t));
  }

  startEdit(lead: Lead): void {
    this.form = {
      name: lead.name,
      phoneNumber: lead.phoneNumber ?? '',
      messengerId: lead.messengerId ?? '',
      source: lead.source ?? '',
    };
    this.editError = '';
    this.editDuplicateId = null;
    this.editing.set(true);
  }

  saveEdit(): void {
    this.api.updateLead(this.id(), {
      name: this.form.name.trim(),
      phoneNumber: this.form.phoneNumber.trim() || null,
      messengerId: this.form.messengerId.trim() || null,
      source: this.form.source || null,
    }).subscribe({
      next: lead => {
        this.editing.set(false);
        this.applied(lead, 'Сохранено');
      },
      error: e => {
        this.editError = errorMessage(e);
        this.editDuplicateId = duplicateId(e);
      },
    });
  }

  moveTo(stageId: string): void {
    if (stageId !== this.lead()?.stageId) {
      this.act(this.api.moveLead(this.id(), stageId));
    }
  }

  assign(managerId: string): void {
    this.act(this.api.assignLeadManager(this.id(), managerId || null), 'Ответственный назначен');
  }

  win(): void {
    this.act(this.api.winLead(this.id()), 'Успешная продажа! Карточка ученика создана');
  }

  lose(): void {
    this.act(this.api.loseLead(this.id(), this.loseReasonId || null, this.loseComment.trim() || null), 'Лид закрыт');
    this.loseOpen.set(false);
  }

  reopen(): void {
    this.act(this.api.reopenLead(this.id()), 'Лид снова в работе');
  }

  setArchived(archived: boolean): void {
    this.act(this.api.setLeadArchived(this.id(), archived), archived ? 'Лид в архиве' : 'Лид возвращён из архива');
  }

  reasonName(id: string | null): string {
    return this.pipeline()?.lostReasons.find(r => r.id === id)?.name ?? 'не указана';
  }

  stageName(id: string | null): string {
    return this.pipeline()?.stages.find(s => s.id === id)?.name ?? 'удалённый этап';
  }

  describe(event: LeadEvent): string {
    switch (event.type) {
      case 'CREATED': return `Этап «${this.stageName(event.toStageId)}»`;
      case 'STAGE_CHANGED': return `${this.stageName(event.fromStageId)} → ${this.stageName(event.toStageId)}`;
      case 'MANAGER_CHANGED': return event.managerId ? `Теперь: ${this.staff.name(event.managerId)}` : 'Ответственный снят';
      case 'LOST': return [event.lostReasonId ? this.reasonName(event.lostReasonId) : '', event.comment ?? '']
        .filter(Boolean).join(' — ');
      default: return '';
    }
  }

  private act(request: ReturnType<ApiService['moveLead']>, success?: string): void {
    request.subscribe({
      next: lead => this.applied(lead, success),
      error: e => this.toast.error(e),
    });
  }

  private applied(lead: Lead, success?: string): void {
    this.lead.set(lead);
    this.loadHistory();
    if (success) {
      this.toast.success(success);
    }
  }
}
