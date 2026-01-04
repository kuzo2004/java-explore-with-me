package ru.practicum.ewm.comment.model;

public enum SortOrder {
    ASC,
    DESC;

    public static SortOrder from(String value) {
        if (value == null) return ASC;
        return SortOrder.valueOf(value.toUpperCase());
    }
}
