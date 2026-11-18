package com.crm.core.entity.enums;

/** Фиксация причины проигрыша лида — настраивается для каждой воронки. */
public enum LostReasonMode {
    DISABLED, // Причина не запрашивается
    OPTIONAL, // Можно указать
    REQUIRED  // Обязательно
}
