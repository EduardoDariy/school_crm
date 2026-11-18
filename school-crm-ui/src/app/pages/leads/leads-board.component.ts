import { CdkDrag, CdkDragDrop, CdkDropList, CdkDropListGroup, transferArrayItem } from '@angular/cdk/drag-drop';
import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { LEAD_STATUS_LABELS } from '../../core/labels';
import { Lead, Pipeline } from '../../core/models';
import { StaffDirectoryService } from '../../core/staff-directory.service';
import { ToastService } from '../../core/toast.service';
import { LeadCreateDialogComponent } from './lead-create-dialog.component';

type View = 'board' | 'closed' | 'archive';

/** Воронка продаж: канбан по этапам с перетаскиванием, закрытые сделки и архив. */
@Component({
  selector: 'app-leads-board',
  imports: [FormsModule, RouterLink, DatePipe, CdkDropListGroup, CdkDropList, CdkDrag, LeadCreateDialogComponent],
  template: `
    <div class="page wide">
      <div class="page-header">
        <h1>Лиды</h1>
        @if (pipelines().length > 1) {
          <select class="input pipeline-select" [ngModel]="pipelineId()" (ngModelChange)="selectPipeline($event)">
            @for (p of pipelines(); track p.id) { <option [value]="p.id">{{ p.name }}</option> }
          </select>
        }
        <span class="spacer"></span>
        <input class="input search" type="search" placeholder="Поиск: имя, телефон, мессенджер"
               [ngModel]="query" (ngModelChange)="onSearch($event)" />
        <label class="checkbox"><input type="checkbox" [(ngModel)]="mine" (ngModelChange)="reload()" /> Только мои</label>
        <button class="btn btn-primary" (click)="createOpen.set(true)">+ Лид</button>
      </div>

      <div class="tabs">
        <button [class.active]="view() === 'board'" (click)="setView('board')">В работе</button>
        <button [class.active]="view() === 'closed'" (click)="setView('closed')">Закрытые</button>
        <button [class.active]="view() === 'archive'" (click)="setView('archive')">Архив</button>
      </div>

      @if (view() === 'board') {
        <div class="board" cdkDropListGroup>
          @for (stage of pipeline()?.stages ?? []; track stage.id) {
            <div class="column">
              <div class="column-head">
                <span class="column-title">{{ stage.name }}</span>
                <span class="badge">{{ columns()[stage.id]?.length ?? 0 }}</span>
              </div>
              <div class="column-body" cdkDropList [id]="stage.id" [cdkDropListData]="columns()[stage.id] ?? []"
                   (cdkDropListDropped)="drop($event)">
                @for (lead of columns()[stage.id] ?? []; track lead.id) {
                  <div class="lead-card" cdkDrag [cdkDragData]="lead" (click)="open(lead)">
                    <div class="lead-name">{{ lead.name }}</div>
                    @if (lead.phoneNumber || lead.messengerId) {
                      <div class="muted small">{{ lead.phoneNumber || lead.messengerId }}</div>
                    }
                    <div class="lead-meta">
                      @if (lead.source) { <span class="badge">{{ lead.source }}</span> }
                      <span class="spacer"></span>
                      <span class="small" [class.muted]="lead.managerId" [class.unassigned]="!lead.managerId">
                        {{ lead.managerId ? staff.name(lead.managerId) : 'Без ответственного' }}
                      </span>
                    </div>
                  </div>
                } @empty {
                  <div class="drop-hint muted small">Перетащите сюда</div>
                }
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="card">
          @if (list().length === 0) { <div class="empty">Ничего не найдено</div> }
          <table class="table">
            @if (list().length > 0) {
              <tr><th>Имя</th><th>Контакт</th><th>Источник</th><th>Статус</th><th>Ответственный</th><th>Создан</th></tr>
            }
            @for (lead of list(); track lead.id) {
              <tr class="clickable" [routerLink]="['/leads', lead.id]">
                <td>{{ lead.name }}</td>
                <td class="muted">{{ lead.phoneNumber || lead.messengerId || '—' }}</td>
                <td>{{ lead.source || '—' }}</td>
                <td><span class="badge" [class.badge-success]="lead.status === 'WON'" [class.badge-danger]="lead.status === 'LOST'">
                  {{ statusLabels[lead.status] }}</span></td>
                <td>{{ staff.name(lead.managerId) }}</td>
                <td class="muted">{{ lead.createdAt | date: 'd MMM y' }}</td>
              </tr>
            }
          </table>
        </div>
      }
    </div>

    @if (createOpen()) {
      <app-lead-create-dialog (closed)="createOpen.set(false)" (created)="createOpen.set(false); reload()" />
    }
  `,
  styles: `
    .wide { max-width: none; }
    .pipeline-select { width: auto; }
    .search { width: 280px; }
    .spacer { flex: 1; }
    .board { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 12px; align-items: flex-start; }
    .column { width: 272px; flex-shrink: 0; background: var(--surface-muted); border-radius: var(--radius); }
    .column-head { display: flex; align-items: center; justify-content: space-between; padding: 10px 12px 6px; }
    .column-title { font-weight: 600; }
    .column-body { min-height: 120px; padding: 4px 8px 10px; display: flex; flex-direction: column; gap: 8px; }
    .lead-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-sm);
      padding: 10px; cursor: pointer; box-shadow: var(--shadow); }
    .lead-card:hover { border-color: var(--accent); }
    .lead-name { font-weight: 500; margin-bottom: 2px; }
    .lead-meta { display: flex; align-items: center; gap: 6px; margin-top: 8px; }
    .unassigned { color: var(--warning); }
    .drop-hint { text-align: center; padding: 24px 0; border: 1px dashed var(--border); border-radius: var(--radius-sm); }
  `,
})
export class LeadsBoardComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  protected readonly staff = inject(StaffDirectoryService);
  protected readonly statusLabels = LEAD_STATUS_LABELS;

  protected readonly pipelines = signal<Pipeline[]>([]);
  protected readonly pipelineId = signal<string | null>(null);
  protected readonly pipeline = computed(() => this.pipelines().find(p => p.id === this.pipelineId()) ?? null);
  protected readonly view = signal<View>('board');
  /** Лиды по этапам; массивы изменяются при перетаскивании. */
  protected readonly columns = signal<Partial<Record<string, Lead[]>>>({});
  protected readonly list = signal<Lead[]>([]);
  protected readonly createOpen = signal(false);

  query = '';
  mine = false;
  private readonly search$ = new Subject<void>();

  constructor() {
    this.search$.pipe(debounceTime(300)).subscribe(() => this.reload());
  }

  ngOnInit(): void {
    this.staff.load();
    this.api.pipelines().subscribe({
      next: pipelines => {
        this.pipelines.set(pipelines);
        this.pipelineId.set((pipelines.find(p => p.defaultPipeline) ?? pipelines[0])?.id ?? null);
        this.reload();
      },
      error: e => this.toast.error(e),
    });
  }

  selectPipeline(id: string): void {
    this.pipelineId.set(id);
    this.reload();
  }

  setView(view: View): void {
    this.view.set(view);
    this.reload();
  }

  onSearch(value: string): void {
    this.query = value;
    this.search$.next();
  }

  reload(): void {
    const pipelineId = this.pipelineId() ?? undefined;
    const base = { pipelineId, mine: this.mine, query: this.query.trim() };
    const view = this.view();
    if (view === 'board') {
      this.api.leads({ ...base, status: 'OPEN' }).subscribe({
        next: leads => {
          const columns: Record<string, Lead[]> = {};
          for (const stage of this.pipeline()?.stages ?? []) {
            columns[stage.id] = [];
          }
          for (const lead of leads) {
            (columns[lead.stageId] ??= []).push(lead);
          }
          this.columns.set(columns);
        },
        error: e => this.toast.error(e),
      });
    } else {
      const request = view === 'archive' ? { ...base, archived: true } : base;
      this.api.leads(request).subscribe({
        next: leads => this.list.set(view === 'closed' ? leads.filter(l => l.status !== 'OPEN') : leads),
        error: e => this.toast.error(e),
      });
    }
  }

  drop(event: CdkDragDrop<Lead[]>): void {
    if (event.previousContainer === event.container) {
      return; // порядок внутри этапа не хранится
    }
    const lead = event.item.data as Lead;
    const stageId = event.container.id;
    transferArrayItem(event.previousContainer.data, event.container.data, event.previousIndex, event.currentIndex);
    this.api.moveLead(lead.id, stageId).subscribe({
      next: updated => Object.assign(lead, updated),
      error: e => {
        this.toast.error(e);
        this.reload();
      },
    });
  }

  open(lead: Lead): void {
    this.router.navigate(['/leads', lead.id]);
  }
}
