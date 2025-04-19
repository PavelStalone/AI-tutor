package rut.uvp.family.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import rut.uvp.family.model.CalendarEvent
import rut.uvp.family.service.CalendarService

@RestController
@RequestMapping("/calendar")
class CalendarController(
    private val calendarService: CalendarService
) {
    data class ImportRequest(val userId: Long, val familyId: Long, val events: List<CalendarEvent>)
    @PostMapping("/import")
    fun importEvents(@RequestBody req: ImportRequest): ResponseEntity<Any> {
        val saved = calendarService.saveEvents(req.userId, req.familyId, req.events)
        return ResponseEntity.ok(saved)
    }
} 