package rut.uvp.family.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KudaGoServiceTest {
    private val service = KudaGoService()

    @Test
    fun testSearchEvents() {
        val params = mapOf("city" to "msk", "categories" to "kids")
        val events = service.searchEvents(params)
        assertTrue(events.isNotEmpty())
    }
}