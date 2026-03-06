package com.frotty27.rpgmobs.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TickClockTest {

    private static final float TICK_DURATION = 1.0f / 30f;

    @Test
    public void advanceAccumulatesTicks() {
        var clock = new TickClock();
        for (int i = 0; i < 30; i++) {
            clock.advance(TICK_DURATION);
        }
        assertEquals(30L, clock.getTick());
    }

    @Test
    public void advancePartialAccumulation() {
        var clock = new TickClock();
        clock.advance(0.02f);
        assertEquals(0L, clock.getTick());
        clock.advance(0.02f);
        assertEquals(1L, clock.getTick());
    }

    @Test
    public void advanceIgnoresZeroAndNegative() {
        var clock = new TickClock();
        clock.advance(0f);
        clock.advance(-1f);
        assertEquals(0L, clock.getTick());
    }

    @Test
    public void getTickReturnsCurrentCount() {
        var clock = new TickClock();
        assertEquals(0L, clock.getTick());
        for (int i = 0; i < 5; i++) {
            clock.advance(TICK_DURATION);
        }
        assertEquals(5L, clock.getTick());
        for (int i = 0; i < 10; i++) {
            clock.advance(TICK_DURATION);
        }
        assertEquals(15L, clock.getTick());
    }
}
