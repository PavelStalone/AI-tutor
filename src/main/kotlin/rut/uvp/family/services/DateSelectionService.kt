package rut.uvp.family.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.SelectedTimeSlot
import java.time.LocalDate

/**
 * Сервис для работы с датами и временными слотами
 */
@Service
class DateSelectionService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    private val timeSlotService: TimeSlotService
) {
    companion object {
        private const val DATE_SELECTION_PROMPT = """
            Ты - помощник по выбору даты для семейных мероприятий.
            
            На основе запроса пользователя и его предпочтений определи наиболее подходящую дату для мероприятия.
            
            Информация о запросе:
            {{requestInfo}}
            
            Если в запросе указаны конкретные даты, используй их. Если указан день недели (например, "суббота"), 
            определи ближайшую дату, соответствующую этому дню недели.
            
            Если используются слова "сегодня", "завтра", "послезавтра", "на этой неделе", "на следующей неделе" - 
            преобразуй их в конкретную дату.
            
            Верни результат в виде даты в формате ISO (YYYY-MM-DD).
            Верни только дату без пояснений.
        """
    }
    
    /**
     * Выбор оптимального временного слота для мероприятия
     */
    fun selectTimeSlot(activityRequest: ActivityRequestData): SelectedTimeSlot? {
        val date = determineDate(activityRequest)
        if (date != null) {
            return timeSlotService.generateTimeSlotsForDate(date).firstOrNull()
        }
        return null
    }
    
    /**
     * Определение даты из запроса пользователя
     */
    private fun determineDate(activityRequest: ActivityRequestData): LocalDate? {
        // Если указана конкретная дата в preferredDate, используем её
        activityRequest.preferredDate?.let {
            return try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        // Если указана конкретная дата в date, используем её
        activityRequest.date?.let {
            return try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        
        // Если указан день недели, определяем ближайшую подходящую дату
        if (activityRequest.dayOfWeek != null) {
            return determineNextDateByDayOfWeek(activityRequest.dayOfWeek)
        }
        
        // Если нет явных указаний на дату, используем ИИ для определения даты на основе контекста
        val requestInfo = buildRequestInfoString(activityRequest)
        val systemPromptText = DATE_SELECTION_PROMPT.replace("{{requestInfo}}", requestInfo)
        
        return try {
            val dateString = chatClient.prompt()
                .system(systemPromptText)
                .user("Определи дату для мероприятия")
                .call()
                .content()
                ?.trim()
                ?: return LocalDate.now()

            LocalDate.parse(dateString)
        } catch (e: Exception) {
            // Если не удалось определить дату, используем сегодня
            LocalDate.now()
        }
    }
    
    /**
     * Определение ближайшей даты по дню недели
     */
    private fun determineNextDateByDayOfWeek(dayOfWeekStr: String): LocalDate {
        val today = LocalDate.now()
        // Отображение русских названий дней недели на числовые значения (1-понедельник, 7-воскресенье)
        val dayOfWeekMap = mapOf(
            "понедельник" to 1, "вторник" to 2, "среда" to 3, "четверг" to 4,
            "пятница" to 5, "суббота" to 6, "воскресенье" to 7
        )
        
        val targetDayOfWeek = dayOfWeekMap[dayOfWeekStr.lowercase()] ?: today.dayOfWeek.value
        var daysToAdd = (targetDayOfWeek - today.dayOfWeek.value)
        if (daysToAdd <= 0) daysToAdd += 7 // Если день уже прошел, берем следующую неделю
        
        return today.plusDays(daysToAdd.toLong())
    }
    
    /**
     * Формирует строку с информацией о запросе для промпта
     */
    private fun buildRequestInfoString(activityRequest: ActivityRequestData): String {
        val sb = StringBuilder()
        
        activityRequest.dayOfWeek?.let { sb.append("День недели: $it\n") }
        
        activityRequest.familyMember?.let { member ->
            val role = member.role ?: "член семьи"
            val age = member.age?.let { " (возраст: $it лет)" } ?: ""
            sb.append("Для кого: $role$age\n")
        }
        
        activityRequest.activityType?.let { sb.append("Тип активности: $it\n") }
        
        if (activityRequest.preferences.isNotEmpty()) {
            sb.append("Предпочтения: ${activityRequest.preferences.joinToString(", ")}\n")
        }
        
        return sb.toString()
    }
}
