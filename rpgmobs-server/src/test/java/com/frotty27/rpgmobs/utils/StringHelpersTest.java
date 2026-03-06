package com.frotty27.rpgmobs.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringHelpersTest {

    @Test
    public void normalizeLowerTrimsAndLowercases() {
        assertEquals("hello world", StringHelpers.normalizeLower("  Hello World  "));
    }

    @Test
    public void normalizeLowerReturnsEmptyForNull() {
        assertEquals("", StringHelpers.normalizeLower(null));
    }

    @Test
    public void normalizeLowerReturnsEmptyForBlank() {
        assertEquals("", StringHelpers.normalizeLower("   "));
    }

    @Test
    public void toDisplayNameConvertsSnakeCase() {
        assertEquals("Charge Leap", StringHelpers.toDisplayName("charge_leap"));
    }

    @Test
    public void toDisplayNameHandlesSingleWord() {
        assertEquals("Heal", StringHelpers.toDisplayName("heal"));
    }

    @Test
    public void toDisplayNameReturnsEmptyForNull() {
        assertEquals("", StringHelpers.toDisplayName(null));
    }
}
