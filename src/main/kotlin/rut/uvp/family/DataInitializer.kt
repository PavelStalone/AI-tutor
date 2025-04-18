package rut.uvp.family

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import rut.uvp.family.models.*
import rut.uvp.family.services.FamilyService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

@Configuration
class DataInitializer {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Bean
    @Order(1)
    fun initData(
        familyService: FamilyService
    ) = CommandLineRunner { _ ->
        println("Checking if sample data already exists...")

        // Проверяем наличие данных
        val existingMembers = familyService.getAllFamilyMembers()
        val existingEvents = familyService.getAllCalendarEvents()

        if (existingMembers.isNotEmpty() || existingEvents.isNotEmpty()) {
            println("Sample data already exists. Skipping initialization.")
            println("Found ${existingMembers.size} family members and ${existingEvents.size} calendar events.")
            return@CommandLineRunner
        }

        println("No existing data found. Initializing sample data for family activity recommendations...")

        initFamilyData(familyService)
        
        println("Sample data initialization completed!")
    }

    private fun getNextWeekendDates(count: Int): List<String> {
        val today = LocalDate.now()
        val dates = mutableListOf<String>()
        
        var date = today
        var weekendsFound = 0
        
        while (weekendsFound < count) {
            date = date.plusDays(1)
            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                dates.add(date.format(dateFormatter))
                weekendsFound++
            }
        }
        
        return dates
    }

    private fun initFamilyData(familyService: FamilyService) {
        familyService.clearAll()
        
        val familyMembers = listOf(
            FamilyMember(
                id = UUID.randomUUID().toString(),
                name = "Александр",
                birthDate = LocalDate.of(1985, 5, 15),
                gender = Gender.MALE,
                preferences = listOf("спорт", "история", "технологии", "путешествия"),
                restrictions = listOf("аллергия на пыльцу"),
                isAccountOwner = true,
                relationToOwner = null
            ),
            FamilyMember(
                id = UUID.randomUUID().toString(),
                name = "Елена",
                birthDate = LocalDate.of(1987, 8, 22),
                gender = Gender.FEMALE,
                preferences = listOf("искусство", "литература", "музыка", "кулинария"),
                restrictions = listOf("непереносимость лактозы"),
                isAccountOwner = false,
                relationToOwner = RelationToOwner.SPOUSE
            ),
            FamilyMember(
                id = UUID.randomUUID().toString(),
                name = "Дмитрий",
                birthDate = LocalDate.of(2010, 3, 10),
                gender = Gender.MALE,
                preferences = listOf("робототехника", "футбол", "настольные игры", "динозавры"),
                restrictions = emptyList(),
                isAccountOwner = false,
                relationToOwner = RelationToOwner.SON
            ),
            FamilyMember(
                id = UUID.randomUUID().toString(),
                name = "Анна",
                birthDate = LocalDate.of(2015, 12, 5),
                gender = Gender.FEMALE,
                preferences = listOf("рисование", "танцы", "мультфильмы", "животные"),
                restrictions = listOf("аллергия на арахис"),
                isAccountOwner = false,
                relationToOwner = RelationToOwner.DAUGHTER
            )
        )
        
        val savedMembers = familyService.addFamilyMembers(familyMembers)
        println("Added ${savedMembers.size} family members")
        
        val calendarEvents = mutableListOf<CalendarEvent>()
        
        val now = LocalDate.now()
        val nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        
        val fatherId = savedMembers[0].id
        calendarEvents.addAll(
            listOf(
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = fatherId,
                    title = "Работа",
                    description = "Рабочие часы",
                    startDateTime = LocalDateTime.of(nextMonday, LocalTime.of(9, 0)),
                    endDateTime = LocalDateTime.of(nextMonday, LocalTime.of(18, 0)),
                    location = "Офис",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=MO,TU,WE,TH,FR",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = fatherId,
                    title = "Тренировка",
                    description = "Спортзал",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(19, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(20, 30)),
                    location = "Спортзал 'Энергия'",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=TU,TH",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = fatherId,
                    title = "Встреча с друзьями",
                    description = "Барбекю у Игоря",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(5), LocalTime.of(16, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(5), LocalTime.of(22, 0)),
                    location = "Дом Игоря",
                    isRecurring = false,
                    isAllDay = false
                )
            )
        )

        val motherId = savedMembers[1].id
        calendarEvents.addAll(
            listOf(
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = motherId,
                    title = "Работа",
                    description = "Рабочие часы",
                    startDateTime = LocalDateTime.of(nextMonday, LocalTime.of(10, 0)),
                    endDateTime = LocalDateTime.of(nextMonday, LocalTime.of(17, 0)),
                    location = "Офис",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=MO,TU,WE,TH,FR",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = motherId,
                    title = "Йога",
                    description = "Занятие йогой",
                    startDateTime = LocalDateTime.of(nextMonday, LocalTime.of(18, 30)),
                    endDateTime = LocalDateTime.of(nextMonday, LocalTime.of(19, 30)),
                    location = "Студия 'Гармония'",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=MO,WE",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = motherId,
                    title = "Книжный клуб",
                    description = "Обсуждение новой книги",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(3), LocalTime.of(19, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(3), LocalTime.of(21, 0)),
                    location = "Библиотека на Пушкинской",
                    isRecurring = false,
                    isAllDay = false
                )
            )
        )

        val sonId = savedMembers[2].id
        calendarEvents.addAll(
            listOf(
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = sonId,
                    title = "Школа",
                    description = "Учебные занятия",
                    startDateTime = LocalDateTime.of(nextMonday, LocalTime.of(8, 30)),
                    endDateTime = LocalDateTime.of(nextMonday, LocalTime.of(14, 30)),
                    location = "Школа №1234",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=MO,TU,WE,TH,FR",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = sonId,
                    title = "Футбол",
                    description = "Тренировка по футболу",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(16, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(17, 30)),
                    location = "Спортивная школа",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=TU,FR",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = sonId,
                    title = "Робототехника",
                    description = "Занятие в кружке робототехники",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(2), LocalTime.of(16, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(2), LocalTime.of(17, 30)),
                    location = "Центр технического творчества",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=WE",
                    isAllDay = false
                )
            )
        )

        val daughterId = savedMembers[3].id
        calendarEvents.addAll(
            listOf(
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = daughterId,
                    title = "Детский сад",
                    description = "Посещение детского сада",
                    startDateTime = LocalDateTime.of(nextMonday, LocalTime.of(8, 0)),
                    endDateTime = LocalDateTime.of(nextMonday, LocalTime.of(17, 0)),
                    location = "Детский сад 'Солнышко'",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=MO,TU,WE,TH,FR",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = daughterId,
                    title = "Танцы",
                    description = "Занятие в танцевальной студии",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(18, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(19, 0)),
                    location = "Студия 'Балерина'",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=TU,TH",
                    isAllDay = false
                ),
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = daughterId,
                    title = "Рисование",
                    description = "Урок рисования",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(2), LocalTime.of(17, 30)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(2), LocalTime.of(18, 30)),
                    location = "Художественная школа",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=WE",
                    isAllDay = false
                )
            )
        )
        
        val savedEvents = familyService.addCalendarEvents(calendarEvents)
        println("Added ${savedEvents.size} calendar events")
    }
}
