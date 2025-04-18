package rut.uvp.family.services

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.ActivitySearchQuery
import rut.uvp.family.models.SelectedTimeSlot
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class SearchQueryService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val SEARCH_QUERY_PROMPT = """
            Ты - помощник для формирования поисковых запросов к API KudaGo для поиска семейных мероприятий.
            
            На основе предоставленной информации о запросе активности, сформируй поисковый запрос и фильтры.
            
            Информация о запросе:
            {{requestInfo}}
            
            Верни результат строго в следующем JSON формате:
            {
              "searchQuery": "строка поискового запроса",
              "filters": {
                "город": "код города (msk для Москвы, spb для Санкт-Петербурга)",
                "категория": "категория мероприятия",
                "бесплатно": "true/false",
                "дата": "YYYY-MM-DD"
              }
            }
            
            Важно:
            - Используй только следующие коды городов: msk (Москва), spb (Санкт-Петербург), nsk (Новосибирск), ekb (Екатеринбург)
            - Если не указан город, используй по умолчанию "msk" (Москва)
            - Категории ограничены: concert, exhibition, theater, movie, education, fashion, party, humor, kids, quest, etc.
            - Поисковый запрос должен быть кратким и содержать ключевые слова для поиска
            
            Верни ТОЛЬКО JSON без пояснений.
        """
    }
    
    /**
     * Генерирует поисковый запрос для KudaGo API
     *
     * @param activityRequest Структурированный запрос активности
     * @param selectedTimeSlot Выбранный временной слот (опционально)
     * @return Поисковый запрос или null, если не удалось сформировать
     */
    fun generateSearchQuery(activityRequest: ActivityRequestData, selectedTimeSlot: SelectedTimeSlot? = null): ActivitySearchQuery? {
        val requestInfo = buildRequestInfoString(activityRequest, selectedTimeSlot)
        val systemPromptText = SEARCH_QUERY_PROMPT.replace("{{requestInfo}}", requestInfo)
        
        return try {
            val jsonResponse = chatClient.prompt()
                .system(systemPromptText)
                .user("Сформируй поисковый запрос для KudaGo API")
                .call()
                .content()
                
            objectMapper.readValue(jsonResponse, ActivitySearchQuery::class.java)
        } catch (e: Exception) {
            println("Error parsing search query: ${e.message}")
            null
        }
    }
    
    /**
     * Формирует строку с информацией о запросе для промпта
     */
    private fun buildRequestInfoString(activityRequest: ActivityRequestData, selectedTimeSlot: SelectedTimeSlot?): String {
        val sb = StringBuilder()
        
        activityRequest.activityType?.let { sb.append("Тип активности: $it\n") }
        
        activityRequest.familyMember?.let { member ->
            val role = member.role ?: "член семьи"
            val age = member.age?.let { " (возраст: $it лет)" } ?: ""
            sb.append("Для кого: $role$age\n")
        }
        
        val date = when {
            activityRequest.preferredDate != null -> "Дата: ${activityRequest.preferredDate}\n"
            activityRequest.date != null -> "Дата: ${activityRequest.date}\n"
            activityRequest.dayOfWeek != null -> "День недели: ${activityRequest.dayOfWeek}\n"
            else -> ""
        }
        sb.append(date)
        
        selectedTimeSlot?.let { sb.append("Временной слот: ${it.selectedTimeRange}\n") }
        
        activityRequest.locationPreference?.let { sb.append("Местоположение: $it\n") }
        activityRequest.budgetConstraint?.let { sb.append("Бюджет: $it\n") }
        
        if (activityRequest.preferences.isNotEmpty()) {
            sb.append("Предпочтения: ${activityRequest.preferences.joinToString(", ")}\n")
        }
        
        if (activityRequest.restrictions.isNotEmpty()) {
            sb.append("Ограничения: ${activityRequest.restrictions.joinToString(", ")}\n")
        }
        
        if (activityRequest.specialRequirements.isNotEmpty()) {
            sb.append("Особые требования: ${activityRequest.specialRequirements.joinToString(", ")}\n")
        }
        
        return sb.toString()
    }
} 