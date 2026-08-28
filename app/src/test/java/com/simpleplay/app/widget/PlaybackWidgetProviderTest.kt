package com.simpleplay.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackWidgetProviderTest {
    @Test
    fun widthMapsToExpectedControlBarLayout() {
        assertEquals(ControlBarWidth.ONE_CELL, controlBarWidthFor(40))
        assertEquals(ControlBarWidth.TWO_CELLS, controlBarWidthFor(110))
        assertEquals(ControlBarWidth.THREE_CELLS, controlBarWidthFor(180))
        assertEquals(ControlBarWidth.FOUR_OR_MORE_CELLS, controlBarWidthFor(250))
    }
}
