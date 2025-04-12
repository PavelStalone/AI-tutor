package rut.uvp.family

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.SelectedTimeSlot
import rut.uvp.family.services.ConversationFlowService
import rut.uvp.family.services.EnhancedConversationFlowService
import rut.uvp.family.services.KudaGoApiService
import rut.uvp.family.services.SearchQueryService
import rut.uvp.family.services.TimeSlotService
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class FamilyActivityTools(
    private val conversationFlowService: EnhancedConversationFlowService,
    private val searchQueryService: SearchQueryService,
    private val timeSlotService: TimeSlotService,
    private val kudaGoApiService: KudaGoApiService,
    private val objectMapper: ObjectMapper,
    private val vectorStore: VectorStore
) {
    /**
     * Находит мероприятия для семейного досуга на основе пользовательского запроса
     *
     * @param query Пользовательский запрос
     * @return Ответ с найденными мероприятиями или запрос на уточнение
     */
    fun findFamilyActivities(query: String): String {
        // Извлекаем данные о запросе активности из сообщения пользователя
        val activityRequest = conversationFlowService.extractActivityRequest(query)
            ?: return "Извините, но я не смог определить ваш запрос. Пожалуйста, уточните, что вы ищете."
         
        // Проверяем, нужна ли дополнительная информация
        val (needsMoreInfo, missingFields) = conversationFlowService.needsMoreInformation(activityRequest)
        if (needsMoreInfo) {
            val familyMemberRole = activityRequest.familyMember?.role
            val followUpQuestion = conversationFlowService.generateFollowUpQuestion(missingFields, familyMemberRole)
            return followUpQuestion
        }
         
        // Автоматически выбираем временной слот, если его нет
        var selectedTimeSlot: SelectedTimeSlot? = null
        if (activityRequest.needsTimeSlotSelection && activityRequest.preferredDate != null) {
            val timeSlots = timeSlotService.generateTimeSlotsForDate(LocalDate.parse(activityRequest.preferredDate))
            // Автоматически выбираем первый временной слот как пример
            selectedTimeSlot = timeSlots.firstOrNull()
        }
         
        // Генерируем поисковый запрос на основе данных
        val searchQuery = searchQueryService.generateSearchQuery(activityRequest, selectedTimeSlot)
            ?: return "Извините, не удалось сформировать поисковый запрос. Пожалуйста, уточните ваши пожелания."
         
        // Ищем мероприятия на KudaGo
        val activities = kudaGoApiService.searchEvents(
            keywords = searchQuery.searchQuery,
            city = searchQuery.filters["город"] ?: "msk",
            isFree = searchQuery.filters["бесплатно"]?.toBoolean(),
            categories = searchQuery.filters["категория"] ?: searchQuery.filters["тип_мероприятия"],
            dateFrom = searchQuery.filters["дата"] ?: activityRequest.preferredDate
        )
         
        // Если ничего не найдено, предлагаем варианты
        if (activities.isEmpty()) {
            return "К сожалению, я не смог найти подходящие мероприятия по вашему запросу. " +
                    "Попробуйте изменить параметры поиска или выбрать другую дату."
        }
         
        // Форматируем ответ
        return formatActivityResponse(activities, searchQuery.searchQuery, activityRequest)
    }
     
    /**
     * Получает доступные города для поиска мероприятий
     *
     * @return Строка с перечислением доступных городов
     */
    fun getAvailableLocations(): String {
        val locations = kudaGoApiService.getAvailableLocations()
        val locationNames = locations.joinToString(", ") { it.name }
        return "Доступные города для поиска мероприятий: $locationNames"
    }
     
    /**
     * Получает доступные категории мероприятий
     *
     * @return Строка с перечислением доступных категорий
     */
    fun getAvailableCategories(): String {
        val categories = kudaGoApiService.getAvailableCategories()
        val categoryNames = categories.joinToString(", ") { it.name }
        return "Доступные категории мероприятий: $categoryNames"
    }
    
    /**
     * Получает информацию о семье из векторного хранилища
     *
     * @return Строка с информацией о семье
     */
    fun getFamilyInformation(): String {
        val searchResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("семья информация о всех членах семьи")
                .filter("type:family_member")
                .topK(10)
                .build()
        )
        
        return if (searchResults.isNullOrEmpty()) {
            "Информация о семье не найдена."
        } else {
            "Информация о вашей семье:\n\n" + 
            searchResults.joinToString("\n\n") { it.text ?: "" }
        }
    }
    
    /**
     * Получает информацию о календаре из векторного хранилища
     *
     * @param memberRole Роль члена семьи (опционально)
     * @return Строка с информацией о календаре
     */
    fun getCalendarInformation(memberRole: String? = null): String {
        val query = memberRole?.let { "календарь $it" } ?: "календарь события"
        val filter = memberRole?.let { "type:calendar_event AND member:$it" } ?: "type:calendar_event"
        
        val searchResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .filter(filter)
                .topK(5)
                .build()
        )
        
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        
        return if (searchResults.isNullOrEmpty()) {
            if (memberRole != null) {
                "В календаре не найдено предстоящих событий для $memberRole."
            } else {
                "В календаре не найдено предстоящих событий."
            }
        } else {
            val header = if (memberRole != null) {
                "Предстоящие события в календаре для $memberRole:\n\n"
            } else {
                "Предстоящие события в календаре:\n\n"
            }
            
            header + searchResults.joinToString("\n\n") { it.text ?: "" }
        }
    }
     
    /**
     * Форматирует ответ о найденных мероприятиях
     *
     * @param activities Список найденных мероприятий
     * @param searchQuery Поисковый запрос
     * @param requestData Данные запроса активности
     * @return Отформатированный текст ответа
     */
    private fun formatActivityResponse(
        activities: List<rut.uvp.family.models.Activity>,
        searchQuery: String,
        requestData: ActivityRequestData
    ): String {
        val formattedActivities = activities.take(5).mapIndexed { index, activity ->
            val title = activity.title
            val description = activity.description.take(150) + if (activity.description.length > 150) "..." else ""
            val dateTime = if (activity.date != null && activity.time != null) {
                "${activity.date}, ${activity.time}"
            } else {
                activity.date ?: "Дата не указана"
            }
            val location = activity.location ?: "Место не указано"
            val price = activity.price ?: "Стоимость не указана"
            val ageRestriction = activity.ageRestriction ?: "Нет ограничений"
            val url = activity.link ?: ""
             
            """
            |${index + 1}. **${title}**
            |📅 ${dateTime}
            |📍 ${location}
            |💰 ${price}
            |👪 ${ageRestriction}
            |${description}
            |Подробнее: ${url}
            """.trimMargin()
        }
         
        val memberInfo = if (requestData.familyMember != null) {
            val role = requestData.familyMember.role
            val age = requestData.familyMember.age
            "${role ?: ""}${if (age != null) " ${age}" else ""}"
        } else {
            ""
        }
         
        val intro = if (memberInfo.isNotEmpty()) {
            "Вот что я нашел для $memberInfo по запросу \"$searchQuery\":"
        } else {
            "Вот что я нашел по запросу \"$searchQuery\":"
        }
         
        return "$intro\n\n${formattedActivities.joinToString("\n\n")}"
    }
} 