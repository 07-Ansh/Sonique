package com.liskovsoft.sharedutils.helpers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class HelpersTest {

    @Test
    public void testNearlyEqual() {
        assertTrue("Values is equal", Helpers.nearlyEqual(59.96f, 60.004f, 0.1f));
        assertTrue("Values is equal", Helpers.nearlyEqual(60.004f, 59.96f, 0.1f));
        assertFalse("Values not equal", Helpers.nearlyEqual(59.96f, 24.554f, 1f));
        assertFalse("Values not equal", Helpers.nearlyEqual(24.554f, 59.96f, 1f));
        assertTrue("Values is equal", Helpers.nearlyEqual(59.96f, 24.554f, 36f));
        assertTrue("Values is equal", Helpers.nearlyEqual(24.554f, 59.96f, 36f));
    }
}
