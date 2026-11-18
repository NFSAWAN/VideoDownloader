package com.app.videodownloader.data.remote

import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class YtDlpMappersTest {

    @Test
    fun `private video errors are recognised`() {
        assertEquals(
            DownloadError.PrivateOrUnavailable,
            mapYtDlpError("ERROR: [facebook] 123: Cannot parse data; this video is private. Use --cookies"),
        )
    }

    @Test
    fun `network errors are recognised`() {
        assertEquals(
            DownloadError.Network,
            mapYtDlpError("ERROR: Unable to download webpage: <urlopen error timed out>"),
        )
    }

    @Test
    fun `anything else is unknown`() {
        assertEquals(DownloadError.Unknown, mapYtDlpError("ERROR: Cannot parse data"))
        assertEquals(DownloadError.Unknown, mapYtDlpError(null))
    }

    @Test
    fun `format selector for best and capped height`() {
        assertEquals("bv*+ba/b", formatSelector(VideoQuality.Best))
        assertEquals("bv*[height<=720]+ba/b[height<=720]/b", formatSelector(VideoQuality(720)))
    }

    @Test
    fun `qualities are best first then distinct heights descending`() {
        assertEquals(
            listOf(VideoQuality.Best, VideoQuality(1080), VideoQuality(720), VideoQuality(360)),
            availableQualities(listOf(360, 720, 0, 1080, 720)),
        )
    }

    @Test
    fun `file names are sanitised and capped`() {
        assertEquals("a b c", sanitizeFileName("a/b: c?", "fallback"))
        assertEquals("fallback", sanitizeFileName("  ///  ", "fallback"))
        assertEquals(80, sanitizeFileName("x".repeat(200), "fallback").length)
    }
}
