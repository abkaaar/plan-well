package com.planwell.app.domain.extraction

import com.planwell.app.domain.model.ExtractionCandidateKind
import com.planwell.app.domain.model.OpportunityDateType
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

data class ParsedDateCandidate(
    val type: OpportunityDateType,
    val extractedDateMs: Long?,
    val label: String?,
    val sourceSnippet: String,
)

data class ParsedTextCandidate(
    val kind: ExtractionCandidateKind,
    val valueText: String,
    val sourceSnippet: String?,
)

data class ParsedOpportunityContent(
    val textCandidates: List<ParsedTextCandidate> = emptyList(),
    val dateCandidates: List<ParsedDateCandidate> = emptyList(),
)

/**
 * Heuristic extraction from OCR / pasted text. All outputs are candidates —
 * never confirmed facts. Does not promote event dates to deadlines without
 * deadline keywords. Ambiguous numeric dates omit [ParsedDateCandidate.extractedDateMs].
 */
object OpportunityTextParser {

    private val urlPattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>"']+|[\w.-]+\.(?:com|org|net|edu|io|co|uk|gov)(?:/[^\s<>"']*)?)""",
    )

    private val deadlineKeywords = listOf(
        "deadline",
        "apply by",
        "application deadline",
        "applications close",
        "applications due",
        "due by",
        "submit by",
        "closing date",
        "last day to apply",
    )

    private val eventKeywords = listOf(
        "event date",
        "event on",
        "happening on",
        "takes place",
        "starts on",
        "start date",
        "workshop on",
        "fair on",
        "conference on",
    )

    private val eligibilityKeywords = listOf(
        "eligibility",
        "eligible",
        "requirements",
        "must be",
        "open to",
        "who can apply",
    )

    private val orgKeywords = listOf(
        "organized by",
        "organised by",
        "hosted by",
        "presented by",
        "sponsored by",
        "university",
        "college",
        "foundation",
        "institute",
    )

    private val monthNames = mapOf(
        "january" to 0, "jan" to 0,
        "february" to 1, "feb" to 1,
        "march" to 2, "mar" to 2,
        "april" to 3, "apr" to 3,
        "may" to 4,
        "june" to 5, "jun" to 5,
        "july" to 6, "jul" to 6,
        "august" to 7, "aug" to 7,
        "september" to 8, "sep" to 8, "sept" to 8,
        "october" to 9, "oct" to 9,
        "november" to 10, "nov" to 10,
        "december" to 11, "dec" to 11,
    )

    fun parse(rawText: String): ParsedOpportunityContent {
        val text = rawText.trim()
        if (text.isBlank()) return ParsedOpportunityContent()

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val textCandidates = mutableListOf<ParsedTextCandidate>()
        val dateCandidates = mutableListOf<ParsedDateCandidate>()

        extractTitle(lines)?.let { textCandidates += it }
        extractUrls(text).forEach { textCandidates += it }
        extractOrganization(lines)?.let { textCandidates += it }
        extractEligibility(lines)?.let { textCandidates += it }
        dateCandidates += extractDates(text, lines)

        return ParsedOpportunityContent(
            textCandidates = textCandidates.distinctBy { it.kind to it.valueText.lowercase(Locale.US) },
            dateCandidates = dateCandidates,
        )
    }

    private fun extractTitle(lines: List<String>): ParsedTextCandidate? {
        val title = lines.firstOrNull { line ->
            line.length in 4..120 &&
                !looksLikeUrl(line) &&
                !isMostlyDate(line) &&
                !deadlineKeywords.any { line.lowercase(Locale.US).contains(it) }
        } ?: return null
        return ParsedTextCandidate(
            kind = ExtractionCandidateKind.TITLE,
            valueText = title,
            sourceSnippet = title,
        )
    }

    private fun extractUrls(text: String): List<ParsedTextCandidate> {
        val matcher = urlPattern.matcher(text)
        val urls = mutableListOf<ParsedTextCandidate>()
        while (matcher.find()) {
            var url = matcher.group(1) ?: continue
            if (url.endsWith(".") || url.endsWith(",")) url = url.dropLast(1)
            val normalized = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
            urls += ParsedTextCandidate(
                kind = ExtractionCandidateKind.URL,
                valueText = normalized,
                sourceSnippet = matcher.group(),
            )
        }
        return urls.distinctBy { it.valueText.lowercase(Locale.US) }
    }

    private fun extractOrganization(lines: List<String>): ParsedTextCandidate? {
        for (line in lines) {
            val lower = line.lowercase(Locale.US)
            for (keyword in orgKeywords) {
                val idx = lower.indexOf(keyword)
                if (idx >= 0) {
                    val after = line.substring(idx + keyword.length).trim(' ', ':', '-', '–')
                    val value = after.ifBlank { line }.take(120)
                    if (value.length >= 3) {
                        return ParsedTextCandidate(
                            kind = ExtractionCandidateKind.ORGANIZATION,
                            valueText = value,
                            sourceSnippet = line,
                        )
                    }
                }
            }
        }
        return null
    }

    private fun extractEligibility(lines: List<String>): ParsedTextCandidate? {
        for (line in lines) {
            val lower = line.lowercase(Locale.US)
            if (eligibilityKeywords.any { lower.contains(it) } && line.length >= 8) {
                return ParsedTextCandidate(
                    kind = ExtractionCandidateKind.ELIGIBILITY,
                    valueText = line.take(300),
                    sourceSnippet = line,
                )
            }
        }
        return null
    }

    private fun extractDates(fullText: String, lines: List<String>): List<ParsedDateCandidate> {
        val results = mutableListOf<ParsedDateCandidate>()
        val lowerFull = fullText.lowercase(Locale.US)

        // Month name dates: "March 15, 2026" / "15 March 2026"
        val monthDayYear = Pattern.compile(
            """(?i)\b((?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?(?:,)?\s+\d{4}|\d{1,2}(?:st|nd|rd|th)?\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+\d{4})\b""",
        )
        val mdMatcher = monthDayYear.matcher(fullText)
        while (mdMatcher.find()) {
            val snippet = mdMatcher.group()
            val ms = parseMonthNameDate(snippet)
            val type = classifyDateType(contextAround(fullText, mdMatcher.start(), mdMatcher.end()), lowerFull)
            results += ParsedDateCandidate(
                type = type,
                extractedDateMs = ms,
                label = snippet,
                sourceSnippet = snippet,
            )
        }

        // ISO-ish: 2026-03-15
        val iso = Pattern.compile("""\b(20\d{2})-(\d{2})-(\d{2})\b""")
        val isoMatcher = iso.matcher(fullText)
        while (isoMatcher.find()) {
            val y = isoMatcher.group(1)!!.toInt()
            val m = isoMatcher.group(2)!!.toInt()
            val d = isoMatcher.group(3)!!.toInt()
            val snippet = isoMatcher.group()
            val ms = toEndOfDayMs(y, m - 1, d)
            val type = classifyDateType(contextAround(fullText, isoMatcher.start(), isoMatcher.end()), lowerFull)
            results += ParsedDateCandidate(
                type = type,
                extractedDateMs = ms,
                label = snippet,
                sourceSnippet = snippet,
            )
        }

        // Numeric slash dates: ambiguous unless clearly day>12 or month name context
        val slash = Pattern.compile("""\b(\d{1,2})[/.](\d{1,2})[/.](20\d{2})\b""")
        val slashMatcher = slash.matcher(fullText)
        while (slashMatcher.find()) {
            val a = slashMatcher.group(1)!!.toInt()
            val b = slashMatcher.group(2)!!.toInt()
            val y = slashMatcher.group(3)!!.toInt()
            val snippet = slashMatcher.group()
            val type = classifyDateType(contextAround(fullText, slashMatcher.start(), slashMatcher.end()), lowerFull)
            val ms = when {
                a > 12 && b in 1..12 -> toEndOfDayMs(y, b - 1, a) // D/M/Y
                b > 12 && a in 1..12 -> toEndOfDayMs(y, a - 1, b) // M/D/Y
                else -> null // ambiguous — keep text only
            }
            results += ParsedDateCandidate(
                type = if (ms == null) OpportunityDateType.OTHER else type,
                extractedDateMs = ms,
                label = snippet,
                sourceSnippet = snippet,
            )
        }

        // If no structured dates but a line is mostly a date phrase, still skip inventing
        if (results.isEmpty()) {
            lines.filter { isMostlyDate(it) }.take(1).forEach { line ->
                results += ParsedDateCandidate(
                    type = OpportunityDateType.OTHER,
                    extractedDateMs = null,
                    label = line.take(80),
                    sourceSnippet = line,
                )
            }
        }

        return results.distinctBy { it.sourceSnippet.lowercase(Locale.US) to it.type }
    }

    private fun classifyDateType(localContext: String, fullLower: String): OpportunityDateType {
        val ctx = localContext.lowercase(Locale.US)
        if (deadlineKeywords.any { ctx.contains(it) }) return OpportunityDateType.APPLICATION_DEADLINE
        if (eventKeywords.any { ctx.contains(it) }) return OpportunityDateType.EVENT
        // Global keyword near start of doc is weak; only use local window — do not
        // infer deadline from a lone event elsewhere in the document.
        if (deadlineKeywords.any { fullLower.contains(it) } &&
            eventKeywords.none { ctx.contains(it) } &&
            ctx.length < 80 &&
            deadlineKeywords.any { fullLower.contains(it) }
        ) {
            // Still require the keyword in a nearby window (already checked local).
        }
        return OpportunityDateType.OTHER
    }

    private fun contextAround(text: String, start: Int, end: Int, radius: Int = 48): String {
        val from = (start - radius).coerceAtLeast(0)
        val to = (end + radius).coerceAtMost(text.length)
        return text.substring(from, to)
    }

    private fun parseMonthNameDate(snippet: String): Long? {
        val lower = snippet.lowercase(Locale.US).replace(",", " ").replace(Regex("""\s+"""), " ").trim()
        val monthFirst = Regex(
            """(?i)(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?\s+(\d{4})""",
        ).matchEntire(lower)
        if (monthFirst != null) {
            val month = monthNames[monthFirst.groupValues[1].lowercase(Locale.US)] ?: return null
            val day = monthFirst.groupValues[2].toInt()
            val year = monthFirst.groupValues[3].toInt()
            return toEndOfDayMs(year, month, day)
        }
        val dayFirst = Regex(
            """(?i)(\d{1,2})(?:st|nd|rd|th)?\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{4})""",
        ).matchEntire(lower)
        if (dayFirst != null) {
            val day = dayFirst.groupValues[1].toInt()
            val month = monthNames[dayFirst.groupValues[2].lowercase(Locale.US)] ?: return null
            val year = dayFirst.groupValues[3].toInt()
            return toEndOfDayMs(year, month, day)
        }
        return null
    }

    private fun toEndOfDayMs(year: Int, monthZeroBased: Int, day: Int): Long? {
        if (monthZeroBased !in 0..11 || day !in 1..31) return null
        return try {
            val cal = Calendar.getInstance(TimeZone.getDefault(), Locale.US).apply {
                isLenient = false
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroBased)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    private fun looksLikeUrl(line: String): Boolean =
        urlPattern.matcher(line).find() || line.contains("www.", ignoreCase = true)

    private fun isMostlyDate(line: String): Boolean {
        val lower = line.lowercase(Locale.US)
        return monthNames.keys.any { lower.contains(it) } &&
            Regex("""\d{4}""").containsMatchIn(line)
    }
}
