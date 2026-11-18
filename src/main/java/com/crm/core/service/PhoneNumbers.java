package com.crm.core.service;

/**
 * Нормализация телефонов для поиска дублей: «+7 (701) 000-00-01» и «+77010000001» — один номер.
 * Казахстанские номера, начинающиеся с 8 (8 701 ...), приводятся к +7.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** Возвращает «+» и цифры, либо null, если номер пуст. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        if (digits.length() < 7 || digits.length() > 15) {
            throw new IllegalArgumentException("Некорректный номер телефона: " + raw);
        }
        return "+" + digits;
    }
}
