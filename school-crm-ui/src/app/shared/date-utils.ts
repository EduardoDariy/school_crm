/** Значение для <input type="datetime-local"> → формат API (LocalDateTime без зоны). */
export function toApiDateTime(local: string): string {
  return local.length === 16 ? `${local}:00` : local;
}

/** Завтра в 10:00 — срок новой задачи по умолчанию, в формате datetime-local. */
export function defaultDueAt(): string {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  date.setHours(10, 0, 0, 0);
  return toLocalInput(date);
}

export function toLocalInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
