package rut.uvp.family.repository

import org.springframework.data.jpa.repository.JpaRepository
import rut.uvp.family.model.CalendarEvent

interface CalendarEventRepository : JpaRepository<CalendarEvent, Long> {
    fun findByFamilyId(familyId: Long): List<CalendarEvent>
    fun findByUserId(userId: Long): List<CalendarEvent>
} 