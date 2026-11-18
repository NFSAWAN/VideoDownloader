package com.app.videodownloader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusItemTest {

    @Test
    fun photosAndVideosAreRecognised() {
        assertEquals("image/jpeg", statusMimeType("abc.jpg"))
        assertEquals("image/jpeg", statusMimeType("abc.JPEG"))
        assertEquals("image/png", statusMimeType("abc.png"))
        assertEquals("video/mp4", statusMimeType("abc.mp4"))
    }

    @Test
    fun otherFilesAreIgnored() {
        assertNull(statusMimeType(".nomedia"))
        assertNull(statusMimeType("abc.gif"))
        assertNull(statusMimeType("noextension"))
    }
}
