/*
 * Заполняет демо-школу тестовыми данными через обычный API (как это сделали бы сотрудники).
 *
 * Запуск без Node.js на хосте (backend должен работать):
 *   docker run --rm -v "$PWD/scripts:/s" node:22-alpine node /s/seed-demo.mjs
 *
 * Переменные окружения:
 *   CRM_API        адрес backend, по умолчанию http://host.docker.internal:8080
 *   CRM_SUBDOMAIN  поддомен школы (demo)
 *   CRM_EMAIL / CRM_PASSWORD  администратор школы (admin@demo.kz / demo12345)
 *
 * Повторный запуск ничего не делает, если сотрудники из набора уже созданы.
 */

const API = (process.env.CRM_API ?? 'http://host.docker.internal:8080').replace(/\/$/, '');
const SUBDOMAIN = process.env.CRM_SUBDOMAIN ?? 'demo';
const ADMIN_EMAIL = process.env.CRM_EMAIL ?? 'admin@demo.kz';
const ADMIN_PASSWORD = process.env.CRM_PASSWORD ?? 'demo12345';
const STAFF_PASSWORD = 'demo12345';
const TZ = 'Asia/Almaty';

// ---------- HTTP ----------

async function call(token, method, path, body) {
  const response = await fetch(`${API}/api${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    const error = new Error(`${method} ${path} → ${response.status}: ${data?.detail ?? text}`);
    error.status = response.status;
    throw error;
  }
  return data;
}

async function login(email, password) {
  const r = await call(null, 'POST', '/auth/login', { subdomain: SUBDOMAIN, email, password });
  return r.accessToken;
}

// ---------- Время школы ----------

/** Дата/время в часовом поясе школы со сдвигом в днях: "YYYY-MM-DDTHH:MM:00". */
function at(dayOffset, time) {
  const date = new Date(Date.now() + dayOffset * 86_400_000);
  const day = date.toLocaleDateString('sv-SE', { timeZone: TZ });
  return `${day}T${time}:00`;
}

function day(dayOffset) {
  return at(dayOffset, '00:00').slice(0, 10);
}

const nowLocal = new Date().toLocaleString('sv-SE', { timeZone: TZ }).replace(' ', 'T');

// ---------- Данные ----------

const STAFF = [
  { key: 'aigerim', email: 'aigerim@demo.kz', fullName: 'Айгерим Садыкова', role: 'MANAGER' },
  { key: 'daniyar', email: 'daniyar@demo.kz', fullName: 'Данияр Ахметов', role: 'MANAGER' },
  { key: 'anna', email: 'anna@demo.kz', fullName: 'Анна Ким', role: 'TEACHER' },
  { key: 'marat', email: 'marat@demo.kz', fullName: 'Марат Есенов', role: 'TEACHER' },
  { key: 'gulnara', email: 'finance@demo.kz', fullName: 'Гульнара Абенова', role: 'FINANCE' },
];

/*
 * owner: кто ведёт лид (null — пока без ответственного).
 * stage: до какого этапа дошёл (0..3). outcome: won / lost / archived.
 * group: в какую группу записать ученика после продажи; lessons: размер абонемента.
 */
const LEADS = [
  // В работе
  { name: 'Алия Нурланова', phone: '+7 701 234 56 01', source: 'Instagram', owner: 'aigerim', stage: 0 },
  { name: 'Ерлан Жумабаев', phone: '+7 702 345 67 02', source: 'WhatsApp', owner: 'aigerim', stage: 1 },
  { name: 'Мадина Касымова', phone: '+7 705 456 78 03', source: 'Сайт', owner: 'aigerim', stage: 2 },
  { name: 'Тимур Оспанов', messenger: '@timur_osp', source: 'Telegram', owner: 'aigerim', stage: 3 },
  { name: 'Сабина Ли', phone: '+7 707 567 89 04', source: 'Рекомендация', owner: 'aigerim', stage: 1 },
  { name: 'Арман Бекетов', phone: '+7 708 678 90 05', source: 'Звонок', owner: 'daniyar', stage: 0 },
  { name: 'Камила Ибраимова', phone: '+7 747 789 01 06', source: 'Instagram', owner: 'daniyar', stage: 2 },
  { name: 'Нурсултан Ермеков', phone: '+7 771 890 12 07', source: 'Сайт', owner: 'daniyar', stage: 3 },
  { name: 'Дана Мухамедова', messenger: '@dana.m', source: 'Instagram', owner: 'daniyar', stage: 1 },
  { name: 'Родион Пак', phone: '+7 775 901 23 08', source: 'WhatsApp', owner: 'daniyar', stage: 2 },
  { name: 'Жанна Серикова', phone: '+7 776 012 34 09', source: 'Instagram', owner: null, stage: 0 },
  { name: 'Максим Ковалёв', phone: '+7 777 123 45 10', source: 'Сайт', owner: null, stage: 0 },
  { name: 'Асель Тулегенова', messenger: '@assel_t', source: 'Telegram', owner: null, stage: 0 },
  { name: 'Ильяс Сулейменов', phone: '+7 778 234 56 11', source: 'Звонок', owner: null, stage: 0 },
  // Успешные продажи → ученики
  { name: 'Айдана Смагулова', phone: '+7 701 111 22 12', source: 'Instagram', owner: 'aigerim', stage: 3, outcome: 'won', group: 'a1', lessons: 16 },
  { name: 'Бауыржан Калиев', phone: '+7 702 222 33 13', source: 'Рекомендация', owner: 'aigerim', stage: 2, outcome: 'won', group: 'a1', lessons: 8 },
  { name: 'Виктория Ни', phone: '+7 705 333 44 14', source: 'Сайт', owner: 'aigerim', stage: 3, outcome: 'won', group: 'a1', lessons: 16 },
  { name: 'Гаухар Даулетова', phone: '+7 707 444 55 15', source: 'WhatsApp', owner: 'daniyar', stage: 3, outcome: 'won', group: 'a1', lessons: 8 },
  { name: 'Дмитрий Соколов', phone: '+7 708 555 66 16', source: 'Звонок', owner: 'daniyar', stage: 2, outcome: 'won', group: 'b1', lessons: 12 },
  { name: 'Еркежан Амирова', phone: '+7 747 666 77 17', source: 'Instagram', owner: 'daniyar', stage: 3, outcome: 'won', group: 'b1', lessons: 6 },
  { name: 'Жандос Омаров', phone: '+7 771 777 88 18', source: 'Сайт', owner: 'aigerim', stage: 3, outcome: 'won', group: 'b1', lessons: 12 },
  { name: 'Зарина Хасенова', phone: '+7 775 888 99 19', source: 'Рекомендация', owner: 'daniyar', stage: 3, outcome: 'won', group: 'ielts', lessons: 8 },
  { name: 'Ислам Абдрахманов', phone: '+7 776 999 00 20', source: 'Instagram', owner: 'aigerim', stage: 2, outcome: 'won', group: 'ielts', lessons: 8 },
  { name: 'Карина Шин', phone: '+7 777 000 11 21', source: 'Сайт', owner: 'daniyar', stage: 3, outcome: 'won', group: 'ielts', lessons: 8 },
  // Проигранные
  { name: 'Лев Морозов', phone: '+7 778 121 21 22', source: 'Сайт', owner: 'aigerim', stage: 2, outcome: 'lost', reason: 'Дорого', comment: 'Нашёл курсы дешевле на 20%' },
  { name: 'Меруерт Жакупова', phone: '+7 701 232 32 23', source: 'Instagram', owner: 'daniyar', stage: 1, outcome: 'lost', reason: 'Не подошло расписание', comment: 'Нужны занятия только по выходным утром' },
  { name: 'Олжас Тоқтаров', messenger: '@olzhas', source: 'Telegram', owner: 'daniyar', stage: 0, outcome: 'lost', reason: 'Не выходит на связь' },
  // Архив
  { name: 'Спам из формы сайта', phone: '+7 700 000 00 00', source: 'Сайт', owner: null, stage: 0, outcome: 'archived' },
];

const GROUPS = [
  { key: 'a1', name: 'English A1 — вечер', teacher: 'anna', days: ['MONDAY', 'WEDNESDAY', 'FRIDAY'], start: '18:30', end: '20:00', topic: 'General English A1' },
  { key: 'b1', name: 'English B1 — утро', teacher: 'marat', days: ['TUESDAY', 'THURSDAY'], start: '09:00', end: '10:30', topic: 'General English B1' },
  { key: 'ielts', name: 'IELTS интенсив', teacher: 'anna', days: ['SATURDAY'], start: '11:00', end: '13:00', topic: 'IELTS Writing & Speaking' },
];

/** Задачи: dayOffset < 0 — просроченные, done — уже выполнены. lead — имя лида. */
const TASKS = [
  { owner: 'aigerim', title: 'Перезвонить и пригласить на пробное', lead: 'Алия Нурланова', dayOffset: -1, time: '11:00' },
  { owner: 'aigerim', title: 'Отправить расписание групп A1', lead: 'Ерлан Жумабаев', dayOffset: 0, time: '23:30' },
  { owner: 'aigerim', title: 'Узнать впечатления после пробного', lead: 'Мадина Касымова', dayOffset: 0, time: '23:45' },
  { owner: 'aigerim', title: 'Выслать договор и реквизиты', lead: 'Тимур Оспанов', dayOffset: 1, time: '10:00' },
  { owner: 'aigerim', title: 'Подтвердить пробное занятие', lead: 'Сабина Ли', dayOffset: 2, time: '12:00' },
  { owner: 'aigerim', title: 'Позвонить по рекомендации', lead: 'Бауыржан Калиев', dayOffset: -6, time: '15:00', done: true },
  { owner: 'daniyar', title: 'Первый звонок, выяснить уровень', lead: 'Арман Бекетов', dayOffset: -2, time: '10:00' },
  { owner: 'daniyar', title: 'Напомнить о пробном завтра', lead: 'Камила Ибраимова', dayOffset: 0, time: '23:30' },
  { owner: 'daniyar', title: 'Согласовать скидку с директором', lead: 'Нурсултан Ермеков', description: 'Просит 10% за оплату сразу 3 месяцев', dayOffset: 1, time: '16:00' },
  { owner: 'daniyar', title: 'Написать в Instagram', lead: 'Дана Мухамедова', dayOffset: 3, time: '11:00' },
  { owner: 'daniyar', title: 'Оформить ученика в группу B1', lead: 'Дмитрий Соколов', dayOffset: -5, time: '12:00', done: true },
  { owner: 'admin', title: 'Распределить новые лиды между менеджерами', dayOffset: 0, time: '23:59' },
  { owner: 'admin', title: 'Проверить посещаемость групп за неделю', dayOffset: 4, time: '18:00' },
];

// ---------- Сценарий ----------

async function main() {
  console.log(`Backend: ${API}, школа «${SUBDOMAIN}»`);
  const admin = await login(ADMIN_EMAIL, ADMIN_PASSWORD);

  const existing = await call(admin, 'GET', '/users');
  if (existing.some(u => u.email === STAFF[0].email)) {
    console.log('Тестовые данные уже есть — ничего не делаю.');
    return;
  }

  // Сотрудники
  const staff = { admin: { token: admin, id: existing.find(u => u.email === ADMIN_EMAIL)?.id } };
  for (const s of STAFF) {
    const user = await call(admin, 'POST', '/users', { email: s.email, fullName: s.fullName, role: s.role, password: STAFF_PASSWORD });
    staff[s.key] = { id: user.id, token: await login(s.email, STAFF_PASSWORD) };
  }
  console.log(`Сотрудники: ${STAFF.length}`);

  // Воронка
  const pipelines = await call(admin, 'GET', '/pipelines');
  const pipeline = pipelines.find(p => p.defaultPipeline) ?? pipelines[0];
  const stages = pipeline.stages;
  const reasonId = name => pipeline.lostReasons.find(r => r.name === name)?.id ?? null;

  // Лиды: создаются ответственным менеджером и проходят этапы по очереди — так появляется история
  const leadIds = {};
  const students = { a1: [], b1: [], ielts: [] };
  for (const spec of LEADS) {
    const token = spec.owner ? staff[spec.owner].token : admin;
    let lead;
    try {
      lead = await call(token, 'POST', '/leads', {
        name: spec.name,
        phoneNumber: spec.phone ?? null,
        messengerId: spec.messenger ?? null,
        source: spec.source,
        pipelineId: pipeline.id,
        stageId: stages[0].id,
      });
    } catch (e) {
      if (e.status === 409) {
        console.log(`  пропущен дубль: ${spec.name}`);
        continue;
      }
      throw e;
    }
    leadIds[spec.name] = lead.id;
    for (let i = 1; i <= spec.stage && i < stages.length; i++) {
      lead = await call(token, 'POST', `/leads/${lead.id}/stage`, { stageId: stages[i].id });
    }
    if (spec.outcome === 'won') {
      lead = await call(token, 'POST', `/leads/${lead.id}/win`);
      students[spec.group].push({ personId: lead.personId, lessons: spec.lessons });
    } else if (spec.outcome === 'lost') {
      await call(token, 'POST', `/leads/${lead.id}/lose`, { lostReasonId: reasonId(spec.reason), comment: spec.comment ?? null });
    } else if (spec.outcome === 'archived') {
      await call(admin, 'POST', `/leads/${lead.id}/archive`);
    }
  }
  console.log(`Лиды: ${Object.keys(leadIds).length}`);

  // Задачи
  for (const t of TASKS) {
    const owner = staff[t.owner];
    const task = await call(owner.token, 'POST', '/tasks', {
      title: t.title,
      description: t.description ?? null,
      dueAt: at(t.dayOffset, t.time),
      assigneeId: owner.id ?? null,
      leadId: t.lead ? leadIds[t.lead] ?? null : null,
    });
    if (t.done) {
      await call(owner.token, 'POST', `/tasks/${task.id}/complete`);
    }
  }
  console.log(`Задачи: ${TASKS.length}`);

  // Группы, расписание, абонементы, посещаемость
  let lessonsCount = 0;
  let marked = 0;
  for (const g of GROUPS) {
    const group = await call(admin, 'POST', '/groups', { name: g.name, teacherId: staff[g.teacher].id });
    const members = students[g.key].filter(s => s.personId);
    for (const s of members) {
      await call(admin, 'POST', `/groups/${group.id}/enroll/${s.personId}`);
      await call(admin, 'POST', '/subscriptions', { studentId: s.personId, groupId: group.id, lessonsTotal: s.lessons });
    }
    const lessons = await call(admin, 'POST', `/groups/${group.id}/lessons/generate`, {
      groupId: group.id,
      startDate: day(-21),
      endDate: day(28),
      daysOfWeek: g.days,
      startTime: `${g.start}:00`,
      endTime: `${g.end}:00`,
      topicTemplate: g.topic,
    });
    lessonsCount += lessons.length;

    // Отметки преподавателя за прошедшие уроки; изредка кто-то пропускает или опаздывает
    const teacher = staff[g.teacher].token;
    const past = lessons.filter(l => l.startTime < nowLocal).sort((a, b) => a.startTime.localeCompare(b.startTime));
    for (const [index, lesson] of past.entries()) {
      const records = members.map((s, i) => {
        const roll = (index * 7 + i * 3) % 11;
        const status = roll === 0 ? 'ABSENT' : roll === 5 ? 'LATE' : 'PRESENT';
        return { studentId: s.personId, status, grade: status === 'ABSENT' ? null : 4 + (roll % 2), note: null };
      });
      if (records.length > 0) {
        await call(teacher, 'POST', `/lessons/${lesson.id}/attendance`, records);
        marked++;
      }
    }
  }
  console.log(`Группы: ${GROUPS.length}, уроков: ${lessonsCount}, отмечено прошедших: ${marked}`);

  const debtors = await call(admin, 'GET', '/reports/debtors');
  console.log(`Должников (закончился абонемент): ${debtors.length}`);
  console.log(`\nГотово. Пароль всех тестовых сотрудников: ${STAFF_PASSWORD}`);
  for (const s of STAFF) {
    console.log(`  ${s.fullName.padEnd(18)} ${s.role.padEnd(8)} ${s.email}`);
  }
}

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
