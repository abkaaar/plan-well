package com.planwell.app.domain.importer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleExtractor @Inject constructor() {

    fun extract(text: String): String {
        val cleaned = text.replace("\r\n", "\n").trim()
        if (cleaned.isEmpty()) return "Untitled"

        val blocks = cleaned.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        for (block in blocks) {
            val candidate = polish(block)
            if (candidate.isEmpty()) continue
            if (isNoiseLine(candidate)) continue
            if (candidate.length <= MAX_TITLE_LEN) return candidate
            // Too long for a title block — fall through to sentence logic using this block
            return firstSentence(candidate).take(MAX_TITLE_LEN).trim()
        }

        return firstSentence(polish(cleaned)).take(MAX_TITLE_LEN).trim()
            .ifEmpty { "Untitled" }
    }

    private fun polish(line: String): String {
        var s = line.trim()
        s = EMOJI_PREFIX.replace(s, "").trim()
        s = TRAILING_HASHTAGS.replace(s, "").trim()
        return s
    }

    private fun isNoiseLine(line: String): Boolean {
        val t = line.trim()
        if (t.startsWith("@")) return true
        if (t.startsWith("#") && !t.contains(' ')) return true
        if (URL_LIKE.containsMatchIn(t) && t.split(Regex("""\s+""")).size <= 2) return true
        if (t.all { it == '#' || it.isWhitespace() || it == '@' }) return true
        return false
    }

    private fun firstSentence(text: String): String {
        val idx = text.indexOfAny(charArrayOf('.', '!', '?', '\n'))
        val sentence = if (idx in 0 until text.lastIndex) {
            text.substring(0, idx + 1).trim()
        } else {
            text.trim()
        }
        return sentence.ifEmpty { text.trim() }
    }

    companion object {
        private const val MAX_TITLE_LEN = 60
        private val EMOJI_PREFIX = Regex(
            """^[\p{So}\p{Cn}\uFE0F\u200D\s]+""",
        )
        private val TRAILING_HASHTAGS = Regex("""(?:\s*#[\w]+)+\s*$""")
        private val URL_LIKE = Regex("""(?i)https?://|www\.|\.com\b|\.ng\b""")
    }
}
