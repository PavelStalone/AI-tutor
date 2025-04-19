package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.CalendarEvent
import rut.uvp.family.repository.CalendarEventRepository
import java.time.LocalDateTime

@Service
class CalendarService(
    private val calendarEventRepository: CalendarEventRepository
) {
    fun saveEvents(userId: Long, familyId: Long, events: List<CalendarEvent>): List<CalendarEvent> {
        return calendarEventRepository.saveAll(events.map { it.copy(userId = userId, familyId = familyId) })
    }
    fun getEventsForFamily(familyId: Long): List<CalendarEvent> = calendarEventRepository.findByFamilyId(familyId)
    fun getEventsForUser(userId: Long): List<CalendarEvent> = calendarEventRepository.findByUserId(userId)
} 