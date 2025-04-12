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

/**
 * REST контроллер для работы с данными о членах семьи и их календарях
 */
@RestController
@RequestMapping("/api/family")
class FamilyController(
    private val familyService: FamilyService
) {
    
    /**
     * Получение всех членов семьи
     * 
     * @return Список всех членов семьи
     */
    @GetMapping("/members")
    fun getAllFamilyMembers(): ResponseEntity<List<FamilyMember>> {
        val members = familyService.getAllFamilyMembers()
        return ResponseEntity.ok(members)
    }
    
    /**
     * Получение члена семьи по ID
     * 
     * @param id ID члена семьи
     * @return Член семьи или ошибка 404, если не найден
     */
    @GetMapping("/members/{id}")
    fun getFamilyMember(@PathVariable id: String): ResponseEntity<FamilyMember> {
        return familyService.getFamilyMember(id)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }
    
    /**
     * Получение владельца аккаунта
     * 
     * @return Член семьи, являющийся владельцем аккаунта, или ошибка 404, если не найден
     */
    @GetMapping("/members/owner")
    fun getAccountOwner(): ResponseEntity<FamilyMember> {
        val owner = familyService.getAllFamilyMembers().firstOrNull { it.isAccountOwner }
        return owner?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }
    
    /**
     * Получение всех событий календаря
     * 
     * @return Список всех событий календаря
     */
    @GetMapping("/calendar")
    fun getAllCalendarEvents(): ResponseEntity<List<CalendarEvent>> {
        val events = familyService.getAllCalendarEvents()
        return ResponseEntity.ok(events)
    }
    
    /**
     * Получение события календаря по ID
     * 
     * @param id ID события
     * @return Событие или ошибка 404, если не найдено
     */
    @GetMapping("/calendar/{id}")
    fun getCalendarEvent(@PathVariable id: String): ResponseEntity<CalendarEvent> {
        return familyService.getCalendarEvent(id)?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.notFound().build()
    }
    
    /**
     * Получение событий календаря для конкретного члена семьи
     * 
     * @param memberId ID члена семьи
     * @return Список событий календаря для указанного члена семьи
     */
    @GetMapping("/calendar/member/{memberId}")
    fun getCalendarEventsForMember(@PathVariable memberId: String): ResponseEntity<List<CalendarEvent>> {
        val events = familyService.getCalendarEventsForFamilyMember(memberId)
        return ResponseEntity.ok(events)
    }
    
    /**
     * Получение событий календаря в указанном диапазоне дат
     * 
     * @param startDate Начальная дата (включительно)
     * @param endDate Конечная дата (включительно)
     * @return Список событий календаря в указанном диапазоне
     */
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