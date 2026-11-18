import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { StaffItem } from './models';

/** Кэш справочника сотрудников: имена ответственных и исполнителей. Доступен администратору и менеджеру. */
@Injectable({ providedIn: 'root' })
export class StaffDirectoryService {
  private readonly api = inject(ApiService);
  private readonly items = signal<StaffItem[]>([]);
  private loaded = false;

  readonly all = this.items.asReadonly();
  readonly active = computed(() => this.items().filter(s => !s.blocked));
  /** Кого можно назначить ответственным за лид. */
  readonly leadOwners = computed(() => this.active().filter(s => s.role === 'MANAGER' || s.role === 'ADMIN'));

  load(force = false): void {
    if (this.loaded && !force) {
      return;
    }
    this.loaded = true;
    this.api.staff().subscribe({ next: list => this.items.set(list), error: () => (this.loaded = false) });
  }

  name(id: string | null | undefined): string {
    if (!id) {
      return '—';
    }
    return this.items().find(s => s.id === id)?.fullName ?? '…';
  }
}
