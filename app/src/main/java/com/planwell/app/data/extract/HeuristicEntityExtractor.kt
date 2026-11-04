package com.planwell.app.data.extract

import androidx.annotation.VisibleForTesting
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.planwell.app.domain.extract.EntityExtractor
import com.planwell.app.domain.extract.EntityType
import com.planwell.app.domain.extract.ExtractedEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeuristicEntityExtractor @Inject constructor(
    private val phoneUtil: PhoneNumberUtil,
) : EntityExtractor {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    @VisibleForTesting
    internal var zoneOverride: ZoneId? = null

    private fun zone(): ZoneId = zoneOverride ?: zoneId

    override suspend fun extract(text: String, referenceTime: Long): List<ExtractedEntity> {
        if (text.isBlank()) return emptyList()
        val ref = ZonedDateTime.ofInstant(Instant.ofEpochMilli(referenceTime), zone())
        return buildList {
            addAll(extractUrls(text))
            addAll(extractPhones(text))
            addAll(extractMoney(text))
            addAll(extractAddresses(text))
            addAll(extractDateTimes(text, ref))
        }.sortedBy { it.start }
    }

    private fun extractUrls(text: String): List<ExtractedEntity> {
        val out = mutableListOf<ExtractedEntity>()
        val matcher = URL_PATTERN.matcher(text)
        while (matcher.find()) {
            var raw = matcher.group() ?: continue
            while (raw.isNotEmpty() && raw.last() in TRAILING_URL_PUNCT) {
                raw = raw.dropLast(1)
            }
            val start = matcher.start()
            val normalised = if (
                raw.startsWith("http://", true) || raw.startsWith("https://", true)
            ) {
                raw
            } else {
                "https://$raw"
            }
            out += ExtractedEntity(EntityType.URL, raw, start, start + raw.length, normalised = normalised)
        }
        return out
    }

    private fun extractPhones(text: String): List<ExtractedEntity> {
        val out = mutableListOf<ExtractedEntity>()
        val matcher = PHONE_PATTERN.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group()?.trim() ?: continue
            val e164 = toE164(raw) ?: continue
            out += ExtractedEntity(
                EntityType.PHONE,
                raw,
                matcher.start(),
                matcher.end(),
                normalised = e164,
            )
        }
        return out
    }

    private fun toE164(raw: String): String? {
        val cleaned = raw.replace(Regex("""[\s\-.]"""), " ").trim()
        val candidates = buildList {
            add(cleaned)
            val compact = cleaned.replace(" ", "")
            add(compact)
            if (compact.startsWith("234") && !compact.startsWith("+")) add("+$compact")
        }
        for (candidate in candidates.distinct()) {
            try {
                val number = phoneUtil.parse(candidate, DEFAULT_REGION)
                if (phoneUtil.isValidNumber(number)) {
                    return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
                }
            } catch (_: NumberParseException) {
                // next
            }
        }
        return null
    }

    private fun extractMoney(text: String): List<ExtractedEntity> {
        val out = mutableListOf<ExtractedEntity>()
        val matcher = MONEY_PATTERN.matcher(text)
        while (matcher.find()) {
            val raw = matcher.group()?.trim() ?: continue
            val normalised = normaliseMoney(raw) ?: continue
            out += ExtractedEntity(
                EntityType.MONEY,
                raw,
                matcher.start(),
                matcher.end(),
                normalised = normalised,
            )
        }
        return out
    }

    private fun normaliseMoney(raw: String): String? {
        val lower = raw.lowercase(Locale.US).replace(",", "").trim()
        val code: String
        var amountPart: String
        when {
            lower.contains('₦') || lower.contains("ngn") ||
                Regex("""(?i)^n\d""").containsMatchIn(lower.replace(" ", "")) -> {
                code = "NGN"
                amountPart = lower
                    .replace("₦", "")
                    .replace(Regex("""(?i)ngn"""), "")
                    .replace(Regex("""(?i)^n\s*"""), "")
                    .trim()
            }
            lower.contains('$') -> {
                code = "USD"
                amountPart = lower.replace("$", "").trim()
            }
            lower.contains('£') -> {
                code = "GBP"
                amountPart = lower.replace("£", "").trim()
            }
            lower.contains('€') -> {
                code = "EUR"
                amountPart = lower.replace("€", "").trim()
            }
            lower.matches(Regex("""[\d.]+[km]""")) -> {
                code = "NGN"
                amountPart = lower
            }
            else -> return null
        }
        val multiplier = when {
            amountPart.endsWith("k") -> 1_000.0
            amountPart.endsWith("m") -> 1_000_000.0
            else -> 1.0
        }
        val numberText = amountPart.removeSuffix("k").removeSuffix("m").trim()
        val major = numberText.toDoubleOrNull() ?: return null
        val minor = (major * multiplier * 100.0).toLong()
        return "$code:$minor"
    }

    private fun extractAddresses(text: String): List<ExtractedEntity> {
        val out = mutableListOf<ExtractedEntity>()
        var searchFrom = 0
        for (line in text.split('\n')) {
            val idx = text.indexOf(line, searchFrom)
            searchFrom = if (idx >= 0) idx + line.length else searchFrom
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val lower = trimmed.lowercase(Locale.US)
            val matched = VENUE_KEYWORDS.any { kw ->
                Regex("""\b${Regex.escape(kw)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)
            }
            if (!matched) continue
            val startInLine = line.indexOf(trimmed)
            val start = (if (idx >= 0) idx else 0) + startInLine
            out += ExtractedEntity(EntityType.ADDRESS, trimmed, start, start + trimmed.length)
        }
        return out
    }

    private data class DateHit(
        val raw: String,
        val start: Int,
        val end: Int,
        val date: LocalDate,
        val time: LocalTime?,
        val timeExplicit: Boolean,
    )

    private fun extractDateTimes(rawText: String, ref: ZonedDateTime): List<ExtractedEntity> {
        // Normalise "7.30 pm" → "7:30 pm" so times are not confused with decimals.
        val text = TIME_DOT_NORMALISE.replace(rawText) { m ->
            "${m.groupValues[1]}:${m.groupValues[2]}${m.groupValues[3]}"
        }
        val hits = mutableListOf<DateHit>()
        val occupied = BooleanArray(text.length)

        fun tryAdd(hit: DateHit): Boolean {
            if (hit.start < 0 || hit.end > text.length || hit.start >= hit.end) return false
            for (i in hit.start until hit.end) if (occupied[i]) return false
            for (i in hit.start until hit.end) occupied[i] = true
            hits += hit
            return true
        }

        fun each(pattern: Pattern, block: (Matcher) -> Unit) {
            val m = pattern.matcher(text)
            while (m.find()) block(m)
        }

        each(RELATIVE_PATTERN) { m ->
            val raw = m.group() ?: return@each
            val resolved = resolveRelative(raw, ref) ?: return@each
            tryAdd(DateHit(raw, m.start(), m.end(), resolved.first, resolved.second, resolved.third))
        }

        each(RANGE_PATTERN) { m ->
            val day = m.group(1)?.toIntOrNull() ?: return@each
            val month = parseMonth(m.group(3) ?: return@each) ?: return@each
            val year = m.group(4)?.toIntOrNull()
            val date = resolveDayMonth(day, month, year, ref.toLocalDate()) ?: return@each
            tryAdd(DateHit(m.group(), m.start(), m.end(), date, null, false))
        }

        each(ISO_PATTERN) { m ->
            val date = runCatching {
                LocalDate.of(m.group(1)!!.toInt(), m.group(2)!!.toInt(), m.group(3)!!.toInt())
            }.getOrNull() ?: return@each
            tryAdd(DateHit(m.group(), m.start(), m.end(), date, null, false))
        }

        each(SLASH_PATTERN) { m ->
            val a = m.group(1)!!.toInt()
            val b = m.group(2)!!.toInt()
            val yRaw = m.group(3)
            val year = yRaw?.let { expandYear(it) }
            val parts = resolveDayMonthParts(a, b) ?: return@each
            val date = resolveDayMonth(parts.first, parts.second, year, ref.toLocalDate())
                ?: return@each
            tryAdd(DateHit(m.group(), m.start(), m.end(), date, null, false))
        }

        each(NAMED_MONTH_PATTERN) { m ->
            val raw = m.group() ?: return@each
            val date = parseNamedMonth(raw, ref.toLocalDate()) ?: return@each
            tryAdd(DateHit(raw, m.start(), m.end(), date, null, false))
        }

        each(ORDINAL_PATTERN) { m ->
            val day = m.group(1)?.toIntOrNull() ?: return@each
            val date = nextFutureDayOfMonth(day, ref.toLocalDate()) ?: return@each
            tryAdd(DateHit(m.group(), m.start(), m.end(), date, null, false))
        }

        each(DEADLINE_WEEKDAY_PATTERN) { m ->
            val weekday = parseWeekday(m.group(1) ?: return@each) ?: return@each
            val date = nextWeekday(weekday, ref.toLocalDate(), allowToday = true)
            tryAdd(DateHit(m.group(), m.start(), m.end(), date, null, false))
        }

        each(DEADLINE_BEFORE_ORDINAL) { m ->
            val day = m.group(1)?.toIntOrNull() ?: return@each
            val date = nextFutureDayOfMonth(day, ref.toLocalDate()) ?: return@each
            tryAdd(DateHit(m.group(), m.start(), m.end(), date, null, false))
        }

        // Collect times and attach to nearest date hit
        data class TimeHit(val start: Int, val end: Int, val time: LocalTime, val raw: String)
        val times = mutableListOf<TimeHit>()
        each(TIME_PATTERN) { m ->
            val raw = m.group() ?: return@each
            val time = parseTimeToken(raw) ?: return@each
            times += TimeHit(m.start(), m.end(), time, raw)
        }

        val enriched = hits.map { hit ->
            if (hit.timeExplicit) return@map hit
            val nearby = times.minByOrNull { t ->
                when {
                    t.end <= hit.start -> hit.start - t.end
                    t.start >= hit.end -> t.start - hit.end
                    else -> 0
                }
            } ?: return@map hit
            val dist = when {
                nearby.end <= hit.start -> hit.start - nearby.end
                nearby.start >= hit.end -> nearby.start - hit.end
                else -> 0
            }
            if (dist > 48) return@map hit
            val start = minOf(hit.start, nearby.start)
            val end = maxOf(hit.end, nearby.end)
            hit.copy(
                raw = text.substring(start, end),
                start = start,
                end = end,
                time = nearby.time,
                timeExplicit = true,
            )
        }.toMutableList()

        // Standalone "closes 5pm" on reference day if not already claimed
        each(CLOSES_TIME_PATTERN) { m ->
            val time = parseTimeToken(m.group(1) ?: return@each) ?: return@each
            val start = m.start()
            val end = m.end()
            if (enriched.any { it.start < end && it.end > start }) return@each
            enriched += DateHit(m.group(), start, end, ref.toLocalDate(), time, true)
        }

        return enriched.map { hit ->
            val localTime = hit.time ?: LocalTime.of(9, 0)
            val ms = hit.date.atTime(localTime).atZone(zone()).toInstant().toEpochMilli()
            ExtractedEntity(
                type = EntityType.DATE_TIME,
                rawText = hit.raw,
                start = hit.start,
                end = hit.end,
                resolvedTimestampMs = ms,
                normalised = if (hit.timeExplicit) "time_explicit" else "time_default_09",
            )
        }
    }

    private fun resolveRelative(
        raw: String,
        ref: ZonedDateTime,
    ): Triple<LocalDate, LocalTime?, Boolean>? {
        val lower = raw.lowercase(Locale.US).trim()
        val today = ref.toLocalDate()
        return when {
            lower == "today" -> Triple(today, null, false)
            lower == "tonight" -> Triple(today, LocalTime.of(20, 0), true)
            lower == "tomorrow" -> Triple(today.plusDays(1), null, false)
            lower == "this weekend" || lower == "next weekend" ->
                Triple(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)), null, false)
            lower == "next week" -> Triple(today.plusWeeks(1), null, false)
            lower.matches(Regex("""in\s+\d+\s+days""")) -> {
                val n = Regex("""in\s+(\d+)\s+days""").find(lower)!!.groupValues[1].toLong()
                Triple(today.plusDays(n), null, false)
            }
            lower.startsWith("next ") -> {
                val day = parseWeekday(lower.removePrefix("next ").trim()) ?: return null
                Triple(nextWeekday(day, today, allowToday = false), null, false)
            }
            else -> null
        }
    }

    private fun parseNamedMonth(raw: String, ref: LocalDate): LocalDate? {
        val cleaned = raw.replace(",", " ").replace(Regex("""\s+"""), " ").trim()
        val dayFirst = Regex(
            """(?i)^(?:(?:mon|tue|wed|thu|fri|sat|sun)[a-z]*\s+)?(\d{1,2})(?:st|nd|rd|th)?\s+([a-z]+)(?:\s+(\d{2,4}))?$""",
        )
        val monthFirst = Regex(
            """(?i)^(?:(?:mon|tue|wed|thu|fri|sat|sun)[a-z]*\s+)?([a-z]+)\s+(\d{1,2})(?:st|nd|rd|th)?(?:\s+(\d{2,4}))?$""",
        )
        dayFirst.matchEntire(cleaned)?.let { m ->
            val day = m.groupValues[1].toInt()
            val month = parseMonth(m.groupValues[2]) ?: return@let null
            val year = m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { expandYear(it) }
            return resolveDayMonth(day, month, year, ref)
        }
        monthFirst.matchEntire(cleaned)?.let { m ->
            val month = parseMonth(m.groupValues[1]) ?: return@let null
            val day = m.groupValues[2].toInt()
            val year = m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { expandYear(it) }
            return resolveDayMonth(day, month, year, ref)
        }
        return null
    }

    private fun resolveDayMonthParts(a: Int, b: Int): Pair<Int, Int>? = when {
        a > 12 && b in 1..12 -> a to b // day/month
        b > 12 && a in 1..12 -> b to a // month/day written as M/D with D>12
        a in 1..31 && b in 1..12 -> a to b // DD/MM default
        else -> null
    }

    private fun resolveDayMonth(day: Int, month: Int, year: Int?, ref: LocalDate): LocalDate? {
        if (month !in 1..12 || day !in 1..31) return null
        if (year != null) return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        val thisYear = runCatching { LocalDate.of(ref.year, month, day) }.getOrNull() ?: return null
        return if (!thisYear.isBefore(ref)) {
            thisYear
        } else {
            runCatching { LocalDate.of(ref.year + 1, month, day) }.getOrNull()
        }
    }

    private fun nextFutureDayOfMonth(day: Int, ref: LocalDate): LocalDate? {
        var ym = YearMonth.from(ref)
        repeat(16) {
            if (day <= ym.lengthOfMonth()) {
                val candidate = ym.atDay(day)
                if (!candidate.isBefore(ref)) return candidate
            }
            ym = ym.plusMonths(1)
        }
        return null
    }

    private fun nextWeekday(day: DayOfWeek, ref: LocalDate, allowToday: Boolean): LocalDate {
        val sameOrNext = ref.with(TemporalAdjusters.nextOrSame(day))
        return when {
            allowToday && sameOrNext == ref -> ref
            allowToday && sameOrNext.isAfter(ref) -> sameOrNext
            !allowToday && sameOrNext == ref -> ref.with(TemporalAdjusters.next(day))
            else -> if (sameOrNext.isAfter(ref)) sameOrNext else ref.with(TemporalAdjusters.next(day))
        }
    }

    private fun parseMonth(token: String): Int? = MONTHS[token.lowercase(Locale.US).take(3)]

    private fun parseWeekday(token: String): DayOfWeek? = WEEKDAYS[token.lowercase(Locale.US).take(3)]

    private fun expandYear(raw: String): Int {
        val y = raw.toInt()
        return if (raw.length <= 2) 2000 + y else y
    }

    private fun parseTimeToken(raw: String): LocalTime? {
        var cleaned = raw.lowercase(Locale.US)
            .replace(Regex("""doors\s+open\s*"""), "")
            .replace('.', ':')
            .replace(Regex("""\s+"""), "")
            .trim()
        val pm = cleaned.endsWith("pm")
        val am = cleaned.endsWith("am")
        cleaned = cleaned.removeSuffix("am").removeSuffix("pm")
        val parts = cleaned.split(':')
        var hour = parts[0].toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        when {
            pm && hour in 1..11 -> hour += 12
            am && hour == 12 -> hour = 0
        }
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }

    companion object {
        private const val DEFAULT_REGION = "NG"
        private val TRAILING_URL_PUNCT = setOf('.', ',', ';', ':', ')', ']')

        private val MONTHS = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
        )
        private val WEEKDAYS = mapOf(
            "mon" to DayOfWeek.MONDAY, "tue" to DayOfWeek.TUESDAY, "wed" to DayOfWeek.WEDNESDAY,
            "thu" to DayOfWeek.THURSDAY, "fri" to DayOfWeek.FRIDAY, "sat" to DayOfWeek.SATURDAY,
            "sun" to DayOfWeek.SUNDAY,
        )
        private val VENUE_KEYWORDS = listOf(
            "road", "rd", "street", "st", "avenue", "ave", "close", "plaza",
            "hall", "centre", "center", "hotel", "campus",
        )

        private val URL_PATTERN = Pattern.compile(
            """(?i)\b((?:https?://|www\.)[^\s<>"']+|[\w.-]+\.(?:com|org|net|edu|io|co|uk|gov|ng)(?:/[^\s<>"']*)?)""",
        )
        private val PHONE_PATTERN = Pattern.compile(
            """(?<![\w+])(?:\+?234[\s\-.]*)?(?:0)?(?:70|80|81|90|91)\d(?:[\s\-.]*\d){7,8}(?![\w])""",
        )
        private val MONEY_PATTERN = Pattern.compile(
            """(?i)(?:₦\s*[\d,]+(?:\.\d+)?[km]?|\bNGN\s*[\d,]+(?:\.\d+)?[km]?|\bN\s*[\d,]+(?:\.\d+)?[km]?\b|\$\s*[\d,]+(?:\.\d+)?[km]?|£\s*[\d,]+(?:\.\d+)?[km]?|€\s*[\d,]+(?:\.\d+)?[km]?|\b\d+(?:\.\d+)?[km]\b)""",
        )
        private val RELATIVE_PATTERN = Pattern.compile(
            """(?i)\b(?:today|tonight|tomorrow|next\s+week|this\s+weekend|next\s+weekend|in\s+\d+\s+days|next\s+(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun))\b""",
        )
        private val RANGE_PATTERN = Pattern.compile(
            """(?i)\b(\d{1,2})\s*[–\-]\s*(\d{1,2})\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\s+(\d{2,4}))?\b""",
        )
        private val ISO_PATTERN = Pattern.compile("""\b(20\d{2})-(\d{2})-(\d{2})\b""")
        private val SLASH_PATTERN = Pattern.compile("""\b(\d{1,2})[/\-](\d{1,2})[/\-](\d{2,4})\b""")
        private val NAMED_MONTH_PATTERN = Pattern.compile(
            """(?i)\b(?:(?:mon|tue|wed|thu|fri|sat|sun)[a-z]*[,]?\s+)?(?:\d{1,2}(?:st|nd|rd|th)?\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)|(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?)(?:\s+\d{2,4})?\b""",
        )
        private val ORDINAL_PATTERN = Pattern.compile("""(?i)\bthe\s+(\d{1,2})(?:st|nd|rd|th)\b""")
        private val DEADLINE_WEEKDAY_PATTERN = Pattern.compile(
            """(?i)\b(?:by|due|before)\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\b""",
        )
        private val DEADLINE_BEFORE_ORDINAL = Pattern.compile(
            """(?i)\bbefore\s+(\d{1,2})(?:st|nd|rd|th)\b""",
        )
        private val TIME_DOT_NORMALISE = Regex(
            """(?i)(\d{1,2})\.(\d{2})(\s*[ap]m\b)""",
        )
        private val TIME_PATTERN = Pattern.compile(
            """(?i)\b(?:doors\s+open\s+)?(?:\d{1,2}(?::\d{2})?\s*(?:am|pm)|\d{1,2}:\d{2})\b""",
        )
        private val CLOSES_TIME_PATTERN = Pattern.compile(
            """(?i)\b(?:closes|deadline|due)\s+(\d{1,2}(?:[:.]\d{2})?\s*(?:am|pm)|\d{1,2}:\d{2})\b""",
        )
    }
}
