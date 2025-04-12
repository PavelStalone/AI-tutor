package rut.uvp.family

import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.FamilyActivityResponse
import rut.uvp.family.services.ConversationFlowService
import rut.uvp.family.services.DateSelectionService
import rut.uvp.family.services.ParserService
import rut.uvp.family.services.SearchQueryService
import com.fasterxml.jackson.databind.ObjectMapper

@Component
class FamilyActivityTools(
    private val conversationFlowService: ConversationFlowService,
    private val dateSelectionService: DateSelectionService,
    private val searchQueryService: SearchQueryService,
    private val parserService: ParserService,
    private val objectMapper: ObjectMapper
) {
    
    @Tool(description = "Инструмент для поиска семейных мероприятий и досуга. Используй этот инструмент, чтобы найти рекомендации для семейного времяпрепровождения.")
    fun findFamilyActivities(@ToolParam(description = "Запрос пользователя с описанием досуга, для кого ищем, предпочтения, ограничения") userQuery: String): String {
        println("Finding family activities for query: $userQuery")
        
        // Step 1: Extract information from the user message
        val activityRequest = conversationFlowService.extractActivityRequest(userQuery)
        
        // Step 2: Check if more information is needed
        val (needsMoreInfo, missingFields) = conversationFlowService.needsMoreInformation(activityRequest)
        if (needsMoreInfo) {
            val followUpQuestion = conversationFlowService.generateFollowUpQuestion(missingFields)
            return followUpQuestion
        }
        
        // Step 3: Auto-select time slot if needed
        val selectedTimeSlot = if (activityRequest.needsTimeSlotSelection) {
            dateSelectionService.selectTimeSlot(activityRequest)
        } else {
            null
        }
        
        // Step 4: Generate search query
        val searchQuery = searchQueryService.generateSearchQuery(activityRequest, selectedTimeSlot)
            ?: return "К сожалению, я не смог сформировать поисковый запрос на основе предоставленной информации. Пожалуйста, уточните ваши предпочтения."
        
        // Step 5: Search for activities
        val activities = parserService.searchActivities(searchQuery)
        
        if (activities.isEmpty()) {
            return "К сожалению, я не смог найти подходящие мероприятия по вашему запросу. Попробуйте изменить параметры поиска или предпочтения."
        }
        
        // Step 6: Format the results
        val response = FamilyActivityResponse(
            request = activityRequest,
            selectedTimeSlot = selectedTimeSlot,
            activities = activities
        )
        
        return formatActivityResponse(response)
    }
    
    /**
     * Formats the activity response into a human-readable text
     * 
     * @param response The activity response object
     * @return A formatted string with activity recommendations
     */
    private fun formatActivityResponse(response: FamilyActivityResponse): String {
        val sb = StringBuilder()
        
        // Add introduction
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
        
        // Add selected time slot if available
        response.selectedTimeSlot?.let {
            sb.append("🗓️ Рекомендуемое время: ${it.selectedDate} в ${it.selectedTimeRange}\n\n")
        }
        
        // Add activities
        response.activities.forEachIndexed { index, activity ->
            sb.append("${index + 1}. **${activity.title}**\n")
            
            activity.description?.let { sb.append("   ${it}\n") }
            
            // Add details
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