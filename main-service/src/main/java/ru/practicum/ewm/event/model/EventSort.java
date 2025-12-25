package ru.practicum.ewm.event.model;

public enum EventSort {
    EVENT_DATE,
    VIEWS;

    public static EventSort from(String value) {
        if (value == null) {
            return EVENT_DATE;
        }
        try {
            return EventSort.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EVENT_DATE;
        }
    }
}
