package rut.uvp.family

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import rut.uvp.family.models.CalendarEvent
import rut.uvp.family.models.FamilyMember
import rut.uvp.family.services.FamilyService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RestController
@RequestMapping("/api/family")
class FamilyController(
    private val familyService: FamilyService
) {

    @GetMapping("/members")
    fun getAllFamilyMembers(): ResponseEntity<List<FamilyMember>> {
        val members = familyService.getAllFamilyMembers()
        return ResponseEntity.ok(members)
    }

    @GetMapping("/members/{id}")
    fun getFamilyMember(@PathVariable id: String): ResponseEntity<FamilyMember> {
        return familyService.getFamilyMember(id)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/members/owner")
    fun getAccountOwner(): ResponseEntity<FamilyMember> {
        val owner = familyService.getAllFamilyMembers().firstOrNull { it.isAccountOwner }
        return owner?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/calendar")
    fun getAllCalendarEvents(): ResponseEntity<List<CalendarEvent>> {
        val events = familyService.getAllCalendarEvents()
        return ResponseEntity.ok(events)
    }

    @GetMapping("/calendar/{id}")
    fun getCalendarEvent(@PathVariable id: String): ResponseEntity<CalendarEvent> {
        return familyService.getCalendarEvent(id)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/calendar/member/{memberId}")
    fun getCalendarEventsForMember(@PathVariable memberId: String): ResponseEntity<List<CalendarEvent>> {
        val events = familyService.getCalendarEventsForFamilyMember(memberId)
        return ResponseEntity.ok(events)
    }

    @GetMapping("/calendar/range")
    fun getCalendarEventsInRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<CalendarEvent>> {
        val startDateTime = LocalDateTime.of(startDate, LocalTime.MIN)
        val endDateTime = LocalDateTime.of(endDate, LocalTime.MAX)
        val events = familyService.getCalendarEventsInRange(startDateTime, endDateTime)
        return ResponseEntity.ok(events)
    }
}
