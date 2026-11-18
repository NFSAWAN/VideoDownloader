package com.app.videodownloader.domain.usecase

import com.app.videodownloader.domain.model.VideoLink
import javax.inject.Inject

/** Picks the first http(s) URL out of shared text; yt-dlp decides whether the site is supported. */
class ExtractLinkUseCase @Inject constructor() {

    operator fun invoke(sharedText: String?): VideoLink? {
        if (sharedText.isNullOrBlank()) return null
        return URL_REGEX.find(sharedText)
            ?.value
            ?.trimEnd('.', ',', ')', ']')
            ?.let(::VideoLink)
    }

    private companion object {
        val URL_REGEX = Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE)
    }
}
