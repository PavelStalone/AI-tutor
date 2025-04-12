package rut.uvp.family.services

import org.springframework.stereotype.Service
import rut.uvp.family.models.CalendarEvent
import rut.uvp.family.models.FamilyMember
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDateTime
import java.time.LocalDate
import java.util.UUID

/**
 * Сервис для работы с данными о членах семьи и их календарях
 */
@Service
class FamilyService {
    
    // In-memory хранилище членов семьи (id -> FamilyMember)
    private val familyMembers = ConcurrentHashMap<String, FamilyMember>()
    
    // In-memory хранилище событий календаря (id -> CalendarEvent)
    private val calendarEvents = ConcurrentHashMap<String, CalendarEvent>()
    
    /**
     * Добавление нового члена семьи
     * 
     * @param familyMember Информация о члене семьи
     * @return Сохраненный член семьи с присвоенным ID
     */
    fun addFamilyMember(familyMember: FamilyMember): FamilyMember {
        val id = familyMember.id.ifEmpty { UUID.randomUUID().toString() }
        val memberWithId = familyMember.copy(id = id)
        familyMembers[id] = memberWithId
        return memberWithId
    }
    
    /**
     * Добавление нескольких членов семьи одновременно
     * 
     * @param members Список членов семьи
     * @return Список сохраненных членов семьи с присвоенными ID
     */
    fun addFamilyMembers(members: List<FamilyMember>): List<FamilyMember> {
        return members.map { addFamilyMember(it) }
    }
    
    /**
     * Получение члена семьи по ID
     * 
     * @param id ID члена семьи
     * @return Член семьи или null, если не найден
     */
    fun getFamilyMember(id: String): FamilyMember? {
        return familyMembers[id]
    }
    
    /**
     * Получение всех членов семьи
     * 
     * @return Список всех членов семьи
     */
    fun getAllFamilyMembers(): List<FamilyMember> {
        return familyMembers.values.toList()
    }
    
    /**
     * Добавление события в календарь
     * 
     * @param event Информация о событии
     * @return Сохраненное событие с присвоенным ID
     */
    fun addCalendarEvent(event: CalendarEvent): CalendarEvent {
        val id = event.id.ifEmpty { UUID.randomUUID().toString() }
        val eventWithId = event.copy(id = id)
        calendarEvents[id] = eventWithId
        return eventWithId
    }
    
    /**
     * Добавление нескольких событий календаря одновременно
     * 
     * @param events Список событий
     * @return Список сохраненных событий с присвоенными ID
     */
    fun addCalendarEvents(events: List<CalendarEvent>): List<CalendarEvent> {
        return events.map { addCalendarEvent(it) }
    }
    
    /**
     * Получение события календаря по ID
     * 
     * @param id ID события
     * @return Событие или null, если не найдено
     */
    fun getCalendarEvent(id: String): CalendarEvent? {
        return calendarEvents[id]
    }
    
    /**
     * Получение всех событий календаря
     * 
     * @return Список всех событий календаря
     */
    fun getAllCalendarEvents(): List<CalendarEvent> {
        return calendarEvents.values.toList()
    }
    
    /**
     * Получение событий календаря для конкретного члена семьи
     * 
     * @param familyMemberId ID члена семьи
     * @return Список событий календаря для указанного члена семьи
     */
    fun getCalendarEventsForFamilyMember(familyMemberId: String): List<CalendarEvent> {
        return calendarEvents.values.filter { it.familyMemberId == familyMemberId }
    }
    
    /**
     * Получение событий календаря в указанном диапазоне дат
     * 
     * @param start Начальная дата (включительно)
     * @param end Конечная дата (включительно)
     * @return Список событий календаря в указанном диапазоне
     */
    fun getCalendarEventsInRange(start: LocalDateTime, end: LocalDateTime): List<CalendarEvent> {
        return calendarEvents.values.filter { 
            (it.startDateTime.isEqual(start) || it.startDateTime.isAfter(start)) && 
            (it.endDateTime.isEqual(end) || it.endDateTime.isBefore(end))
        }
    }
    
    /**
     * Очистка всех данных (для тестирования)
     */
    fun clearAll() {
        familyMembers.clear()
        calendarEvents.clear()
    }
} 