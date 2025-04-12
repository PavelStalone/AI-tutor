package rut.uvp.family

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import rut.uvp.family.models.*
import rut.uvp.family.services.DateSelectionService
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
    
    /**
     * Initializes sample data for the application
     * This CommandLineRunner will execute when the application starts
     */
    @Bean
    fun initData(
        dateSelectionService: DateSelectionService,
        familyService: FamilyService
    ) = CommandLineRunner { _ ->
        println("Initializing sample data for family activity recommendations...")
        
        // Инициализация данных о временных слотах
        initTimeSlots(dateSelectionService)
        
        // Инициализация данных о членах семьи и их календарях
        initFamilyData(familyService)
        
        println("Sample data initialization completed!")
    }
    
    /**
     * Инициализация данных о временных слотах
     */
    private fun initTimeSlots(dateSelectionService: DateSelectionService) {
        // Get the next few weekend dates
        val nextWeekendDates = getNextWeekendDates(3)
        
        // Add time slots for daughter
        dateSelectionService.addAvailableTimeSlots(
            "дочь",
            listOf(
                "${nextWeekendDates[0]} 10:00-13:00",
                "${nextWeekendDates[0]} 15:00-18:00",
                "${nextWeekendDates[1]} 11:00-14:00",
                "${nextWeekendDates[1]} 16:00-19:00"
            )
        )
        
        // Add time slots for son
        dateSelectionService.addAvailableTimeSlots(
            "сын",
            listOf(
                "${nextWeekendDates[0]} 09:00-12:00",
                "${nextWeekendDates[0]} 14:00-17:00",
                "${nextWeekendDates[1]} 10:00-13:00",
                "${nextWeekendDates[1]} 15:00-18:00"
            )
        )
        
        // Add time slots for spouse
        dateSelectionService.addAvailableTimeSlots(
            "жена",
            listOf(
                "${nextWeekendDates[0]} 18:00-22:00",
                "${nextWeekendDates[1]} 19:00-23:00",
                "${nextWeekendDates[2]} 17:00-21:00"
            )
        )
        
        // Add time slots for spouse (male form)
        dateSelectionService.addAvailableTimeSlots(
            "муж",
            listOf(
                "${nextWeekendDates[0]} 18:00-22:00",
                "${nextWeekendDates[1]} 19:00-23:00",
                "${nextWeekendDates[2]} 17:00-21:00"
            )
        )
    }
    
    /**
     * Инициализация данных о членах семьи и их календарях
     */
    private fun initFamilyData(familyService: FamilyService) {
        // Очищаем предыдущие данные
        familyService.clearAll()
        
        // Создаем семью из 4 человек: родители и двое детей
        val familyMembers = listOf(
            // Владелец аккаунта - отец
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
            // Мать - жена владельца
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
            // Сын
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
            // Дочь
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
        
        // Сохраняем членов семьи
        val savedMembers = familyService.addFamilyMembers(familyMembers)
        println("Added ${savedMembers.size} family members")
        
        // Создаем моковые данные календаря
        val calendarEvents = mutableListOf<CalendarEvent>()
        
        // Получаем текущую дату
        val now = LocalDate.now()
        val nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        
        // События для отца (Александр)
        val fatherId = savedMembers[0].id
        calendarEvents.addAll(
            listOf(
                // Рабочие часы (повторяющиеся с понедельника по пятницу)
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
                // Тренировка (вторник и четверг)
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
                // Встреча с друзьями (в субботу)
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
        
        // События для матери (Елена)
        val motherId = savedMembers[1].id
        calendarEvents.addAll(
            listOf(
                // Рабочие часы (повторяющиеся с понедельника по пятницу)
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
                // Йога (понедельник и среда)
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
                // Книжный клуб (в четверг)
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
        
        // События для сына (Дмитрий)
        val sonId = savedMembers[2].id
        calendarEvents.addAll(
            listOf(
                // Школа (повторяющееся с понедельника по пятницу)
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
                // Футбольная секция (вторник и пятница)
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
                // Кружок робототехники (среда)
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
        
        // События для дочери (Анна)
        val daughterId = savedMembers[3].id
        calendarEvents.addAll(
            listOf(
                // Детский сад (повторяющееся с понедельника по пятницу)
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = daughterId,
                    title = "Детский сад",
                    description = "Пребывание в детском саду",
                    startDateTime = LocalDateTime.of(nextMonday, LocalTime.of(8, 0)),
                    endDateTime = LocalDateTime.of(nextMonday, LocalTime.of(18, 0)),
                    location = "Детский сад №56",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=MO,TU,WE,TH,FR",
                    isAllDay = false
                ),
                // Танцевальный кружок (вторник и четверг)
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = daughterId,
                    title = "Танцы",
                    description = "Занятие танцами",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(18, 30)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(1), LocalTime.of(19, 30)),
                    location = "Дворец творчества",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=TU,TH",
                    isAllDay = false
                ),
                // Рисование (суббота)
                CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    familyMemberId = daughterId,
                    title = "Рисование",
                    description = "Кружок рисования",
                    startDateTime = LocalDateTime.of(nextMonday.plusDays(5), LocalTime.of(10, 0)),
                    endDateTime = LocalDateTime.of(nextMonday.plusDays(5), LocalTime.of(11, 30)),
                    location = "Художественная школа",
                    isRecurring = true,
                    recurrenceRule = "WEEKLY;BYDAY=SA",
                    isAllDay = false
                )
            )
        )
        
        // Семейные события для всех членов семьи
        val familyEvents = listOf(
            // Семейный ужин (пятница)
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                familyMemberId = fatherId, // Привязываем к отцу для примера
                title = "Семейный ужин",
                description = "Ужин со всей семьей",
                startDateTime = LocalDateTime.of(nextMonday.plusDays(4), LocalTime.of(19, 0)),
                endDateTime = LocalDateTime.of(nextMonday.plusDays(4), LocalTime.of(20, 30)),
                location = "Дом",
                isRecurring = true,
                recurrenceRule = "WEEKLY;BYDAY=FR",
                isAllDay = false
            ),
            // Посещение бабушки и дедушки (воскресенье)
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                familyMemberId = fatherId, // Привязываем к отцу для примера
                title = "Поездка к бабушке и дедушке",
                description = "Посещение родителей",
                startDateTime = LocalDateTime.of(nextMonday.plusDays(6), LocalTime.of(12, 0)),
                endDateTime = LocalDateTime.of(nextMonday.plusDays(6), LocalTime.of(18, 0)),
                location = "Дом родителей",
                isRecurring = false,
                isAllDay = false
            ),
            // День рождения сына
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                familyMemberId = sonId,
                title = "День рождения Дмитрия",
                description = "Празднование дня рождения",
                startDateTime = LocalDateTime.of(LocalDate.of(LocalDate.now().year, 3, 10), LocalTime.of(16, 0)),
                endDateTime = LocalDateTime.of(LocalDate.of(LocalDate.now().year, 3, 10), LocalTime.of(21, 0)),
                location = "Дом",
                isRecurring = false,
                isAllDay = false
            ),
            // День рождения дочери
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                familyMemberId = daughterId,
                title = "День рождения Анны",
                description = "Празднование дня рождения",
                startDateTime = LocalDateTime.of(LocalDate.of(LocalDate.now().year, 12, 5), LocalTime.of(16, 0)),
                endDateTime = LocalDateTime.of(LocalDate.of(LocalDate.now().year, 12, 5), LocalTime.of(20, 0)),
                location = "Детский развлекательный центр",
                isRecurring = false,
                isAllDay = false
            )
        )
        
        // Добавляем семейные события
        calendarEvents.addAll(familyEvents)
        
        // Сохраняем все события календаря
        val savedEvents = familyService.addCalendarEvents(calendarEvents)
        println("Added ${savedEvents.size} calendar events")
    }
    
    /**
     * Gets the dates of the next N weekends (Saturday and Sunday)
     *
     * @param count Number of weekend days to return
     * @return List of dates in "YYYY-MM-DD" format
     */
    private fun getNextWeekendDates(count: Int): List<String> {
        val weekendDates = mutableListOf<String>()
        var currentDate = LocalDate.now()
        
        while (weekendDates.size < count) {
            currentDate = currentDate.plusDays(1)
            val dayOfWeek = currentDate.dayOfWeek.value
            
            // Check if it's Saturday (6) or Sunday (7)
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                weekendDates.add(currentDate.format(dateFormatter))
            }
        }
        
        return weekendDates
    }
} 