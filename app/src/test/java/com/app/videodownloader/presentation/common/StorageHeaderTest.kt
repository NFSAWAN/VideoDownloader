package com.app.videodownloader.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageHeaderTest {

    @Test
    fun formatsSizesByMagnitude() {
        assertEquals("0 B", formatSize(0))
        assertEquals("999 B", formatSize(999))
        assertEquals("1 KB", formatSize(1_000))
        assertEquals("2.5 MB", formatSize(2_500_000))
        assertEquals("1.4 GB", formatSize(1_400_000_000))
    }
}
