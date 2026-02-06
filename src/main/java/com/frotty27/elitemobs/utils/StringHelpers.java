package com.frotty27.elitemobs.utils;

import java.util.Locale;

public final class StringHelpers {

    private StringHelpers() {
    }

    public static String toLowerOrEmpty(String value) {
        return (value == null) ? "" : value.toLowerCase(Locale.ROOT);
    }

    public static String normalizeLower(String value) {
        return (value == null) ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
