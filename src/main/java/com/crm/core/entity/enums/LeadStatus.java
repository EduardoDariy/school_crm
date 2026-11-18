package com.crm.core.entity.enums;

public enum LeadStatus {
    OPEN, // В работе: находится на одном из этапов воронки
    WON,  // Успешно: создана карточка ученика
    LOST  // Проигран: с причиной, если так настроена воронка
}
