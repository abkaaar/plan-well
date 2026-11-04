package com.planwell.app.data.extract

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.planwell.app.domain.extract.EntityType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

class HeuristicEntityExtractorPhoneTest {

    private lateinit var extractor: HeuristicEntityExtractor

    @Before
    fun setUp() {
        extractor = HeuristicEntityExtractor(PhoneNumberUtil.getInstance()).also {
            it.zoneOverride = ZoneId.of("Africa/Lagos")
        }
    }

    @Test
    fun localNgFormat() = runTest {
        val phone = extractor.extract("Call 08031234567 now", 0L)
            .firstOrNull { it.type == EntityType.PHONE }
        assertEquals("+2348031234567", phone?.normalised)
    }

    @Test
    fun plus234Format() = runTest {
        val phone = extractor.extract("Reach +2348031234567", 0L)
            .firstOrNull { it.type == EntityType.PHONE }
        assertEquals("+2348031234567", phone?.normalised)
    }

    @Test
    fun bare234WithSpaces() = runTest {
        val phone = extractor.extract("WhatsApp 234 803 123 4567 thanks", 0L)
            .firstOrNull { it.type == EntityType.PHONE }
        assertEquals("+2348031234567", phone?.normalised)
    }

    @Test
    fun invalidNumber_omitted() = runTest {
        val phone = extractor.extract("Code 12345 only", 0L)
            .firstOrNull { it.type == EntityType.PHONE }
        assertNull(phone)
        assertTrue(extractor.extract("Call 0803123", 0L).none { it.type == EntityType.PHONE })
    }

    @Test
    fun moneyAndUrlSmoke() = runTest {
        val entities = extractor.extract(
            "Fee ₦5,000 or N5000 / 5k. Site www.example.com/apply. Meet at City Hall campus.",
            0L,
        )
        assertTrue(entities.any { it.type == EntityType.MONEY && it.normalised == "NGN:500000" })
        assertTrue(entities.any { it.type == EntityType.URL && it.normalised!!.startsWith("https://") })
        assertTrue(entities.any { it.type == EntityType.ADDRESS && it.rawText.contains("Hall", true) })
    }
}
