package rut.uvp.family.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rut.uvp.family.model.CalendarEvent
import rut.uvp.family.service.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/mock")
class MockDataController(
    private val userService: UserService,
    private val familyService: FamilyService,
    private val familyMemberService: FamilyMemberService,
    private val calendarService: CalendarService
) {
    @GetMapping("/fill")
    fun fillMockData(): String {
        // 1. Создаём пользователей
        val user1 = userService.register("mom@example.com", "password", "Мама")
        val user2 = userService.register("dad@example.com", "password", "Папа")
        val user3 = userService.register("son@example.com", "password", "Сын")
        // 2. Создаём семью
        val family = familyService.createFamily(user1.id)
        // 3. Добавляем членов семьи
        familyMemberService.addMember(user1.id, family.id, "мама")
        familyMemberService.addMember(user2.id, family.id, "папа")
        familyMemberService.addMember(user3.id, family.id, "сын")
        // 4. Добавляем события календаря
        val now = LocalDateTime.now()
        val events = listOf(
            CalendarEvent(userId = user1.id, familyId = family.id, title = "Работа", start = now.plusDays(1).withHour(9), end = now.plusDays(1).withHour(17), source = "mock"),
            CalendarEvent(userId = user2.id, familyId = family.id, title = "Встреча", start = now.plusDays(1).withHour(14), end = now.plusDays(1).withHour(15), source = "mock"),
            CalendarEvent(userId = user3.id, familyId = family.id, title = "Школа", start = now.plusDays(1).withHour(8), end = now.plusDays(1).withHour(13), source = "mock")
        )
        calendarService.saveEvents(user1.id, family.id, events.filter { it.userId == user1.id })
        calendarService.saveEvents(user2.id, family.id, events.filter { it.userId == user2.id })
        calendarService.saveEvents(user3.id, family.id, events.filter { it.userId == user3.id })
        return "Mock data filled: familyId=${family.id}"
    }
} 