import { Component, HostListener, input, output } from '@angular/core';

/** Модальное окно: заголовок, содержимое и кнопки через ng-content. Закрывается по Esc и фону. */
@Component({
  selector: 'app-modal',
  template: `
    <div class="backdrop" (mousedown)="closed.emit()">
      <div class="dialog" [style.width.px]="width()" (mousedown)="$event.stopPropagation()" role="dialog">
        <header>
          <h2>{{ title() }}</h2>
          <button class="btn btn-link close" type="button" (click)="closed.emit()" aria-label="Закрыть">✕</button>
        </header>
        <div class="body"><ng-content /></div>
        <footer><ng-content select="[actions]" /></footer>
      </div>
    </div>
  `,
  styles: `
    .backdrop { position: fixed; inset: 0; background: rgba(16, 24, 40, .45); display: flex;
      align-items: flex-start; justify-content: center; padding: 8vh 16px; z-index: 100; overflow-y: auto; }
    .dialog { background: var(--surface); border-radius: 10px; box-shadow: var(--shadow-lg); max-width: 100%; }
    header { display: flex; align-items: center; padding: 16px 20px 0; }
    header h2 { flex: 1; margin: 0; }
    .close { color: var(--text-muted); font-size: 16px; }
    .body { padding: 16px 20px; }
    footer { display: flex; justify-content: flex-end; gap: 8px; padding: 0 20px 16px; }
    footer:empty { display: none; }
  `,
})
export class ModalComponent {
  readonly title = input.required<string>();
  readonly width = input(480);
  readonly closed = output<void>();

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.closed.emit();
  }
}
