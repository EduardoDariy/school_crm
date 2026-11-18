import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { LostReason, LostReasonMode, Pipeline, Stage } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { ModalComponent } from '../../shared/modal.component';

const MODES: { value: LostReasonMode; label: string }[] = [
  { value: 'REQUIRED', label: 'Обязательна' },
  { value: 'OPTIONAL', label: 'По желанию' },
  { value: 'DISABLED', label: 'Не спрашивать' },
];

/** Настройка воронки: этапы (порядок, названия, удаление с переносом лидов) и причины отказа. */
@Component({
  selector: 'app-pipeline-settings',
  imports: [FormsModule, CdkDropList, CdkDrag, CdkDragHandle, ModalComponent],
  template: `
    <div class="page narrow">
      <div class="page-header"><h1>Настройки воронки</h1></div>
      @if (pipeline(); as p) {
        <section class="card">
          <div class="form-grid">
            <label class="field"><span>Название воронки</span>
              <input class="input" [(ngModel)]="name" (blur)="savePipeline(p)" (keyup.enter)="savePipeline(p)" />
            </label>
            <label class="field"><span>Причина отказа при проигрыше лида</span>
              <select class="input" [(ngModel)]="mode" (ngModelChange)="savePipeline(p)">
                @for (m of modes; track m.value) { <option [value]="m.value">{{ m.label }}</option> }
              </select>
            </label>
          </div>
        </section>

        <div class="grid-2 sections">
          <section class="card">
            <h2>Этапы</h2>
            <p class="muted small">Перетащите, чтобы изменить порядок. «Успешно» и «Проигран» — не этапы, а итог сделки.</p>
            <ul class="items" cdkDropList (cdkDropListDropped)="reorder(p, $event)">
              @for (stage of p.stages; track stage.id) {
                <li cdkDrag>
                  <span class="handle" cdkDragHandle title="Перетащить">⋮⋮</span>
                  <input class="input" [ngModel]="stage.name" #stageName
                         (blur)="renameStage(p, stage, stageName.value)" (keyup.enter)="stageName.blur()" />
                  <button class="btn btn-sm btn-danger" (click)="askDelete(stage)" [disabled]="p.stages.length <= 1"
                          title="Удалить этап">✕</button>
                </li>
              }
            </ul>
            <form class="row add" (ngSubmit)="addStage(p)">
              <input class="input" name="newStage" [(ngModel)]="newStage" placeholder="Новый этап" />
              <button class="btn" type="submit" [disabled]="!newStage.trim()">Добавить</button>
            </form>
          </section>

          <section class="card">
            <h2>Причины отказа</h2>
            <p class="muted small">Отключённые причины не предлагаются, но остаются в истории и отчётах.</p>
            <ul class="items">
              @for (reason of p.lostReasons; track reason.id) {
                <li [class.inactive]="!reason.active">
                  <input class="input" [ngModel]="reason.name" #reasonName
                         (blur)="updateReason(p, reason, reasonName.value, reason.active)" (keyup.enter)="reasonName.blur()" />
                  <label class="checkbox small">
                    <input type="checkbox" [checked]="reason.active"
                           (change)="updateReason(p, reason, reason.name, !reason.active)" /> активна
                  </label>
                </li>
              }
            </ul>
            <form class="row add" (ngSubmit)="addReason(p)">
              <input class="input" name="newReason" [(ngModel)]="newReason" placeholder="Новая причина" />
              <button class="btn" type="submit" [disabled]="!newReason.trim()">Добавить</button>
            </form>
          </section>
        </div>
      }
    </div>

    @if (deleting(); as stage) {
      <app-modal title="Удалить этап «{{ stage.name }}»" [width]="420" (closed)="deleting.set(null)">
        <div class="stack">
          <div>Если на этапе есть лиды, их нужно перенести на другой этап.</div>
          <label class="field"><span>Перенести лиды на</span>
            <select class="input" [(ngModel)]="moveTo">
              @for (s of otherStages(stage); track s.id) { <option [value]="s.id">{{ s.name }}</option> }
            </select>
          </label>
        </div>
        <ng-container actions>
          <button class="btn" (click)="deleting.set(null)">Отмена</button>
          <button class="btn btn-primary" (click)="deleteStage(stage)">Удалить</button>
        </ng-container>
      </app-modal>
    }
  `,
  styles: `
    .narrow { max-width: 1000px; }
    .sections { margin-top: 16px; }
    .items { list-style: none; margin: 0 0 12px; padding: 0; display: flex; flex-direction: column; gap: 6px; }
    .items li { display: flex; align-items: center; gap: 8px; background: var(--surface); }
    .items li.inactive .input { color: var(--text-muted); text-decoration: line-through; }
    .handle { cursor: grab; color: var(--text-muted); padding: 0 2px; user-select: none; letter-spacing: -2px; }
    .add .input { flex: 1; width: auto; }
    .checkbox { white-space: nowrap; }
    p { margin: -6px 0 12px; }
  `,
})
export class PipelineSettingsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly toast = inject(ToastService);
  protected readonly modes = MODES;

  protected readonly pipeline = signal<Pipeline | null>(null);
  protected readonly deleting = signal<Stage | null>(null);

  name = '';
  mode: LostReasonMode = 'REQUIRED';
  newStage = '';
  newReason = '';
  moveTo = '';

  ngOnInit(): void {
    this.api.pipelines().subscribe({
      next: list => this.apply(list.find(p => p.defaultPipeline) ?? list[0] ?? null),
      error: e => this.toast.error(e),
    });
  }

  savePipeline(p: Pipeline): void {
    if (!this.name.trim() || (this.name.trim() === p.name && this.mode === p.lostReasonMode)) {
      return;
    }
    this.run(this.api.updatePipeline(p.id, { name: this.name.trim(), lostReasonMode: this.mode }));
  }

  reorder(p: Pipeline, event: CdkDragDrop<unknown>): void {
    if (event.previousIndex === event.currentIndex) {
      return;
    }
    const stages = [...p.stages];
    moveItemInArray(stages, event.previousIndex, event.currentIndex);
    this.pipeline.set({ ...p, stages });
    this.run(this.api.reorderStages(p.id, stages.map(s => s.id)), 'Порядок этапов сохранён');
  }

  renameStage(p: Pipeline, stage: Stage, name: string): void {
    if (name.trim() && name.trim() !== stage.name) {
      this.run(this.api.renameStage(p.id, stage.id, name.trim()), 'Этап переименован');
    }
  }

  addStage(p: Pipeline): void {
    this.run(this.api.addStage(p.id, this.newStage.trim()), 'Этап добавлен');
    this.newStage = '';
  }

  askDelete(stage: Stage): void {
    this.moveTo = this.otherStages(stage)[0]?.id ?? '';
    this.deleting.set(stage);
  }

  otherStages(stage: Stage): Stage[] {
    return (this.pipeline()?.stages ?? []).filter(s => s.id !== stage.id);
  }

  deleteStage(stage: Stage): void {
    const p = this.pipeline();
    if (p) {
      this.run(this.api.deleteStage(p.id, stage.id, this.moveTo || undefined), 'Этап удалён');
    }
    this.deleting.set(null);
  }

  addReason(p: Pipeline): void {
    this.run(this.api.addLostReason(p.id, this.newReason.trim()), 'Причина добавлена');
    this.newReason = '';
  }

  updateReason(p: Pipeline, reason: LostReason, name: string, active: boolean): void {
    if (!name.trim() || (name.trim() === reason.name && active === reason.active)) {
      return;
    }
    this.run(this.api.updateLostReason(p.id, reason.id, name.trim(), active));
  }

  private run(request: ReturnType<ApiService['addStage']>, success?: string): void {
    request.subscribe({
      next: p => {
        this.apply(p);
        if (success) {
          this.toast.success(success);
        }
      },
      error: e => {
        this.toast.error(e);
        this.ngOnInit();
      },
    });
  }

  private apply(p: Pipeline | null): void {
    this.pipeline.set(p);
    if (p) {
      this.name = p.name;
      this.mode = p.lostReasonMode;
    }
  }
}
