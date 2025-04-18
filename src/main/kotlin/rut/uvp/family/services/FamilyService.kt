package rut.uvp.family.services

import org.springframework.stereotype.Service
import rut.uvp.family.models.CalendarEvent
import rut.uvp.family.models.FamilyMember
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.UUID

@Service
class FamilyService {
    
    private val familyMembers = ConcurrentHashMap<String, FamilyMember>()
    
    private val calendarEvents = ConcurrentHashMap<String, CalendarEvent>()

    fun addFamilyMember(familyMember: FamilyMember): FamilyMember {
        val id = familyMember.id.ifEmpty { UUID.randomUUID().toString() }
        val memberWithId = familyMember.copy(id = id)
        familyMembers[id] = memberWithId
        return memberWithId
    }

    fun addFamilyMembers(members: List<FamilyMember>): List<FamilyMember> {
        return members.map { addFamilyMember(it) }
    }

    fun getFamilyMember(id: String): FamilyMember? {
        return familyMembers[id]
    }

    fun getAllFamilyMembers(): List<FamilyMember> {
        return familyMembers.values.toList()
    }

    fun addCalendarEvent(event: CalendarEvent): CalendarEvent {
        val id = event.id.ifEmpty { UUID.randomUUID().toString() }
        val eventWithId = event.copy(id = id)
        calendarEvents[id] = eventWithId
        return eventWithId
    }
    
    fun addCalendarEvents(events: List<CalendarEvent>): List<CalendarEvent> {
        return events.map { addCalendarEvent(it) }
    }
    
    fun getCalendarEvent(id: String): CalendarEvent? {
        return calendarEvents[id]
    }
    
    fun getAllCalendarEvents(): List<CalendarEvent> {
        return calendarEvents.values.toList()
    }
    
    fun getCalendarEventsForFamilyMember(familyMemberId: String): List<CalendarEvent> {
        return calendarEvents.values.filter { it.familyMemberId == familyMemberId }
    }
    
    fun getCalendarEventsInRange(start: LocalDateTime, end: LocalDateTime): List<CalendarEvent> {
        return calendarEvents.values.filter { 
            (it.startDateTime.isEqual(start) || it.startDateTime.isAfter(start)) && 
            (it.endDateTime.isEqual(end) || it.endDateTime.isBefore(end))
        }
    }
    
    fun clearAll() {
        familyMembers.clear()
        calendarEvents.clear()
    }
}
