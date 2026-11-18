package com.app.videodownloader.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractLinkUseCaseTest {

    private val extractLink = ExtractLinkUseCase()

    @Test
    fun `extracts facebook url from share text`() {
        val link = extractLink("Check this out https://www.facebook.com/watch/?v=123456 wow")
        assertEquals("https://www.facebook.com/watch/?v=123456", link?.url)
    }

    @Test
    fun `accepts fb watch short links`() {
        assertEquals("https://fb.watch/abc123/", extractLink("https://fb.watch/abc123/")?.url)
    }

    @Test
    fun `rejects non facebook urls`() {
        assertNull(extractLink("https://example.com/facebook.com"))
    }

    @Test
    fun `rejects blank and null`() {
        assertNull(extractLink(null))
        assertNull(extractLink("  "))
    }
}
