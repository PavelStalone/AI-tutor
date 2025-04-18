package rut.uvp.family

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.FamilyActivityResponse
import rut.uvp.family.services.EnhancedConversationFlowService
import rut.uvp.family.services.SearchQueryService
import rut.uvp.family.services.TimeSlotService
import rut.uvp.family.services.ParserService
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import org.slf4j.LoggerFactory

@Component
class FamilyActivityTools(
    private val conversationFlowService: EnhancedConversationFlowService,
    private val timeSlotService: TimeSlotService,
    private val searchQueryService: SearchQueryService,
    private val parserService: ParserService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(FamilyActivityTools::class.java)
    
    @Tool(description = "Инструмент для поиска семейных мероприятий и досуга. Используй этот инструмент, чтобы найти рекомендации для семейного времяпрепровождения.")
    suspend fun findFamilyActivities(@ToolParam(description = "Запрос пользователя с описанием досуга, для кого ищем, предпочтения, ограничения") userQuery: String): String {
        logger.info("Finding family activities for query: $userQuery")
        
        // Извлекаем данные о запросе активности из сообщения пользователя
        val activityRequest = conversationFlowService.extractActivityRequest(userQuery)
        
        if (activityRequest == null) {
            logger.warn("Failed to extract activity request from user query")
            return "К сожалению, я не смог определить ваш запрос. Пожалуйста, уточните, что вы ищете."
        }
        
        try {
            logger.info("Extracted activity request: ${objectMapper.writeValueAsString(activityRequest)}")
        } catch (e: Exception) {
            logger.warn("Failed to serialize activity request for logging")
        }
        
        // Проверяем, нужна ли дополнительная информация
        val (needsMoreInfo, missingFields) = conversationFlowService.needsMoreInformation(activityRequest)
        if (needsMoreInfo) {
            logger.info("Need more information. Missing fields: $missingFields")
            val familyMemberRole = activityRequest.familyMember?.role
            val followUpQuestion = conversationFlowService.generateFollowUpQuestion(missingFields, familyMemberRole)
            return followUpQuestion
        }
        
        // Автоматически выбираем временной слот, если его нет
        val selectedTimeSlot = if (activityRequest.needsTimeSlotSelection && activityRequest.preferredDate != null) {
            timeSlotService.generateTimeSlotsForDate(LocalDate.parse(activityRequest.preferredDate)).firstOrNull()
        } else {
            null
        }
        
        // Генерируем поисковый запрос на основе данных
        val searchQuery = searchQueryService.generateSearchQuery(activityRequest, selectedTimeSlot)
        
        if (searchQuery == null) {
            logger.warn("Failed to generate search query")
            return "К сожалению, я не смог сформировать поисковый запрос на основе предоставленной информации. Пожалуйста, уточните ваши предпочтения."
        }
        
        try {
            logger.info("Generated search query: ${objectMapper.writeValueAsString(searchQuery)}")
        } catch (e: Exception) {
            logger.warn("Failed to serialize search query for logging")
        }
        
        // Ищем мероприятия
        logger.info("Searching activities with query: ${searchQuery.searchQuery}")
        val activities = parserService.searchActivities(searchQuery)
        
        logger.info("Found ${activities.size} activities")
        
        if (activities.isEmpty()) {
            return "К сожалению, я не смог найти подходящие мероприятия по вашему запросу. Попробуйте изменить параметры поиска или предпочтения."
        }
        
        val response = FamilyActivityResponse(
            request = activityRequest,
            selectedTimeSlot = selectedTimeSlot,
            activities = activities
        )
        
        return formatActivityResponse(response)
    }

    private fun formatActivityResponse(response: FamilyActivityResponse): String {
        val sb = StringBuilder()
        
        val familyMember = response.request.familyMember
        if (familyMember != null) {
            sb.append("Вот что я нашел для ")
            sb.append(familyMember.role ?: "вашего ребенка")
            if (familyMember.age != null) {
                sb.append(" ${familyMember.age} лет")
            }
            sb.append(":\n\n")
        } else {
            sb.append("Вот что я нашел:\n\n")
        }
        
        response.selectedTimeSlot?.let {
            sb.append("🗓️ Рекомендуемое время: ${it.selectedDate} в ${it.selectedTimeRange}\n\n")
        }
        
        response.activities.forEachIndexed { index, activity ->
            sb.append("${index + 1}. **${activity.title}**\n")
            
            activity.description?.let { sb.append("   ${it}\n") }
            
            val details = mutableListOf<String>()
            
            activity.date?.let { details.add("Дата: $it") }
            activity.time?.let { details.add("Время: $it") }
            activity.location?.let { details.add("Где: $it") }
            activity.price?.let { details.add("Цена: $it") }
            activity.ageRestriction?.let { details.add("Возраст: $it") }
            
            if (details.isNotEmpty()) {
                sb.append("   _${details.joinToString(" | ")}_\n")
            }
            
            activity.url?.let { sb.append("   🔗 [Подробнее]($it)\n") }
            
            sb.append("\n")
        }
        
        return sb.toString()
    }
}
