package rut.uvp.family

import org.springframework.ai.document.Document
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import com.fasterxml.jackson.databind.ObjectMapper
import rut.uvp.family.models.FamilyMember
import rut.uvp.family.models.CalendarEvent
import rut.uvp.family.services.FamilyService
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.Period

/**
 * Класс для инициализации RAG системы данными о семье
 */
@Configuration
class FamilyRagInitializer(
    private val objectMapper: ObjectMapper
) {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    /**
     * Инициализирует векторное хранилище данными о семье
     */
    @Bean
    @Order(2) // Выполняется после очистки хранилища
    fun initFamilyRag(
        vectorStore: VectorStore,
        familyService: FamilyService
    ) = CommandLineRunner { _ ->
        println("Initializing RAG with family data...")
        
        // Получаем всех членов семьи
        val members = familyService.getAllFamilyMembers()
        
        // Создаем документы о каждом члене семьи
        val memberDocuments = members.flatMap { member ->
            createMemberDocuments(member)
        }
        
        // Получаем все события календаря
        val events = familyService.getAllCalendarEvents()
        
        // Создаем документы о событиях календаря
        val eventDocuments = events.flatMap { event ->
            // Находим участника события
            val member = familyService.getFamilyMember(event.familyMemberId)
            createEventDocuments(event, member)
        }
        
        // Объединяем документы
        val allDocuments = memberDocuments + eventDocuments
        
        // Создаем токенизатор для разбиения документов
        val textSplitter = TokenTextSplitter(
            maxTokenCount = 500,
            minTokenCount = 200,
            overlapTokenCount = 20,
            maxSegmentSize = 5000,
            keepSeparators = false
        )
        
        // Разбиваем документы на сегменты
        val splitDocuments = textSplitter.split(allDocuments)
        
        // Добавляем документы в векторное хранилище
        val addedCount = vectorStore.add(splitDocuments)
        
        println("Added $addedCount documents to RAG store")
        println("RAG initialization completed!")
    }
    
    /**
     * Создает документы с информацией о члене семьи
     */
    private fun createMemberDocuments(member: FamilyMember): List<Document> {
        val documents = mutableListOf<Document>()
        
        // Основная информация о члене семьи
        val age = Period.between(member.birthDate, LocalDate.now()).years
        val baseInfo = """
            Информация о члене семьи:
            Имя: ${member.name}
            Возраст: $age лет
            Дата рождения: ${member.birthDate.format(dateFormatter)}
            Пол: ${formatGender(member.gender)}
            ${if (member.isAccountOwner) "Владелец аккаунта" else "Кем приходится владельцу аккаунта: ${formatRelation(member.relationToOwner)}"}
        """.trimIndent()
        
        val baseDocument = Document.builder()
            .id("member_base_${member.id}")
            .text(baseInfo)
            .metadata(
                mapOf(
                    "type" to "family_member",
                    "member_id" to member.id,
                    "member_name" to member.name,
                    "member_age" to age.toString(),
                    "member_gender" to member.gender.toString(),
                    "is_account_owner" to member.isAccountOwner.toString()
                )
            )
            .build()
        documents.add(baseDocument)
        
        // Предпочтения и ограничения
        if (member.preferences.isNotEmpty() || member.restrictions.isNotEmpty()) {
            val preferencesInfo = """
                Предпочтения и ограничения ${member.name}:
                ${if (member.preferences.isNotEmpty()) "Предпочтения: ${member.preferences.joinToString(", ")}" else ""}
                ${if (member.restrictions.isNotEmpty()) "Ограничения: ${member.restrictions.joinToString(", ")}" else ""}
            """.trimIndent()
            
            val preferencesDocument = Document.builder()
                .id("member_preferences_${member.id}")
                .text(preferencesInfo)
                .metadata(
                    mapOf(
                        "type" to "member_preferences",
                        "member_id" to member.id,
                        "member_name" to member.name,
                        "preferences" to member.preferences.joinToString(","),
                        "restrictions" to member.restrictions.joinToString(",")
                    )
                )
                .build()
            documents.add(preferencesDocument)
        }
        
        // Особенности по возрасту
        val ageSpecificInfo = when (age) {
            in 0..2 -> "Для младенцев и детей до 2 лет подходят простые игрушки, яркие картинки, музыкальные занятия и короткие прогулки."
            in 3..6 -> "Дети 3-6 лет любят творчество, простые игры, мультфильмы, посещение детских площадок и интерактивные занятия."
            in 7..12 -> "Дети 7-12 лет интересуются активными играми, мастер-классами, познавательными мероприятиями, квестами и начинают увлекаться хобби."
            in 13..17 -> "Подростки 13-17 лет предпочитают общение со сверстниками, компьютерные игры, спорт, музыку и мероприятия соответствующие их интересам."
            in 18..25 -> "Молодые люди 18-25 лет интересуются активным отдыхом, ночной жизнью, концертами, фестивалями и путешествиями."
            in 26..40 -> "Взрослые 26-40 лет часто предпочитают культурные мероприятия, спорт, хобби и семейный отдых."
            in 41..60 -> "Взрослые 41-60 лет интересуются искусством, театром, выставками, спокойным отдыхом на природе и экскурсиями."
            else -> "Пожилые люди старше 60 лет предпочитают спокойный отдых, посещение парков, театров, концертов классической музыки и встречи с друзьями."
        }
        
        val ageDocument = Document.builder()
            .id("member_age_specific_${member.id}")
            .text("Особенности досуга для ${member.name}, возраст $age лет: $ageSpecificInfo")
            .metadata(
                mapOf(
                    "type" to "age_specific",
                    "member_id" to member.id,
                    "member_name" to member.name,
                    "member_age" to age.toString()
                )
            )
            .build()
        documents.add(ageDocument)
        
        return documents
    }
    
    /**
     * Создает документы с информацией о событиях календаря
     */
    private fun createEventDocuments(event: CalendarEvent, member: FamilyMember?): List<Document> {
        val documents = mutableListOf<Document>()
        
        // Базовая информация о событии
        val memberName = member?.name ?: "Неизвестный участник"
        val eventInfo = """
            Событие в календаре ${memberName}:
            Название: ${event.title}
            Описание: ${event.description ?: ""}
            Дата и время: ${event.startDateTime.format(dateFormatter)} ${event.startDateTime.format(timeFormatter)} - ${event.endDateTime.format(timeFormatter)}
            Место: ${event.location ?: "Не указано"}
            ${if (event.isRecurring) "Повторяющееся событие: ${formatRecurrenceRule(event.recurrenceRule)}" else "Единовременное событие"}
        """.trimIndent()
        
        val eventDocument = Document.builder()
            .id("event_${event.id}")
            .text(eventInfo)
            .metadata(
                mapOf(
                    "type" to "calendar_event",
                    "event_id" to event.id,
                    "event_title" to event.title,
                    "member_id" to event.familyMemberId,
                    "member_name" to memberName,
                    "event_date" to event.startDateTime.format(dateFormatter),
                    "event_start_time" to event.startDateTime.format(timeFormatter),
                    "event_end_time" to event.endDateTime.format(timeFormatter),
                    "is_recurring" to event.isRecurring.toString()
                )
            )
            .build()
        documents.add(eventDocument)
        
        // Если событие повторяющееся, добавляем информацию о доступности
        if (event.isRecurring) {
            val availabilityInfo = when {
                event.title.contains("Работа", ignoreCase = true) -> 
                    "$memberName работает с ${event.startDateTime.format(timeFormatter)} до ${event.endDateTime.format(timeFormatter)} по будним дням, поэтому в это время недоступен(на) для семейных мероприятий."
                
                event.title.contains("Школа", ignoreCase = true) || 
                event.title.contains("Детский сад", ignoreCase = true) ->
                    "$memberName находится в ${event.title} с ${event.startDateTime.format(timeFormatter)} до ${event.endDateTime.format(timeFormatter)} по будним дням, поэтому в это время недоступен(на) для семейных мероприятий."
                
                event.recurrenceRule?.contains("SA", ignoreCase = true) == true || 
                event.recurrenceRule?.contains("SU", ignoreCase = true) == true ->
                    "$memberName занимается ${event.title} в выходные с ${event.startDateTime.format(timeFormatter)} до ${event.endDateTime.format(timeFormatter)}, поэтому в это время недоступен(на) для других семейных мероприятий."
                
                else ->
                    "$memberName регулярно занимается ${event.title} и недоступен(на) для семейных мероприятий в это время: ${event.startDateTime.format(timeFormatter)} - ${event.endDateTime.format(timeFormatter)}."
            }
            
            val availabilityDocument = Document.builder()
                .id("event_availability_${event.id}")
                .text(availabilityInfo)
                .metadata(
                    mapOf(
                        "type" to "availability",
                        "member_id" to event.familyMemberId,
                        "member_name" to memberName,
                        "event_id" to event.id,
                        "event_title" to event.title
                    )
                )
                .build()
            documents.add(availabilityDocument)
        }
        
        return documents
    }
    
    /**
     * Форматирует пол в текстовом виде
     */
    private fun formatGender(gender: rut.uvp.family.models.Gender): String {
        return when (gender) {
            rut.uvp.family.models.Gender.MALE -> "Мужской"
            rut.uvp.family.models.Gender.FEMALE -> "Женский"
            rut.uvp.family.models.Gender.OTHER -> "Другой"
        }
    }
    
    /**
     * Форматирует отношение к владельцу аккаунта в текстовом виде
     */
    private fun formatRelation(relation: rut.uvp.family.models.RelationToOwner?): String {
        return when (relation) {
            rut.uvp.family.models.RelationToOwner.SPOUSE -> "Супруг(а)"
            rut.uvp.family.models.RelationToOwner.SON -> "Сын"
            rut.uvp.family.models.RelationToOwner.DAUGHTER -> "Дочь"
            rut.uvp.family.models.RelationToOwner.FATHER -> "Отец"
            rut.uvp.family.models.RelationToOwner.MOTHER -> "Мать"
            rut.uvp.family.models.RelationToOwner.BROTHER -> "Брат"
            rut.uvp.family.models.RelationToOwner.SISTER -> "Сестра"
            rut.uvp.family.models.RelationToOwner.GRANDFATHER -> "Дедушка"
            rut.uvp.family.models.RelationToOwner.GRANDMOTHER -> "Бабушка"
            rut.uvp.family.models.RelationToOwner.OTHER -> "Другое"
            null -> "Не указано"
        }
    }
    
    /**
     * Форматирует правило повторения в человекочитаемом виде
     */
    private fun formatRecurrenceRule(rule: String?): String {
        if (rule == null) return "Нет правила повторения"
        
        return when {
            rule.contains("WEEKLY;BYDAY=MO,TU,WE,TH,FR") -> "Каждый будний день (пн-пт)"
            rule.contains("WEEKLY;BYDAY=MO,WE,FR") -> "По понедельникам, средам и пятницам"
            rule.contains("WEEKLY;BYDAY=TU,TH") -> "По вторникам и четвергам"
            rule.contains("WEEKLY;BYDAY=MO,WE") -> "По понедельникам и средам"
            rule.contains("WEEKLY;BYDAY=TU,FR") -> "По вторникам и пятницам"
            rule.contains("WEEKLY;BYDAY=SA,SU") -> "По выходным (сб-вс)"
            rule.contains("WEEKLY;BYDAY=MO") -> "Каждый понедельник"
            rule.contains("WEEKLY;BYDAY=TU") -> "Каждый вторник"
            rule.contains("WEEKLY;BYDAY=WE") -> "Каждую среду"
            rule.contains("WEEKLY;BYDAY=TH") -> "Каждый четверг"
            rule.contains("WEEKLY;BYDAY=FR") -> "Каждую пятницу"
            rule.contains("WEEKLY;BYDAY=SA") -> "Каждую субботу"
            rule.contains("WEEKLY;BYDAY=SU") -> "Каждое воскресенье"
            rule.contains("WEEKLY") -> "Еженедельно"
            rule.contains("MONTHLY") -> "Ежемесячно"
            rule.contains("DAILY") -> "Ежедневно"
            else -> rule
        }
    }
} 