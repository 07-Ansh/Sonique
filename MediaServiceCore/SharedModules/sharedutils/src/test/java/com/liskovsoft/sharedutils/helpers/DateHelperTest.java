package com.liskovsoft.sharedutils.helpers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DateHelperTest {
    @Test
    public void testToUnixTimeMs() {
        long timeMs = DateHelper.toUnixTimeMs("2022-09-11T23:39:38+00:00");
        assertEquals("time equals", 1662939578000L, timeMs);

        long timeMs2 = DateHelper.toUnixTimeMs("2022-09-16T04:39:06");
        assertTrue("time not null", timeMs2 > 0);
    }
}