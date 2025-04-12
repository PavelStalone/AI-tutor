package rut.uvp.family.services

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class ConversationFlowService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val EXTRACTION_PROMPT = """
            Ты помощник для планирования семейного досуга. Твоя задача - извлечь из сообщения пользователя информацию о запросе и вернуть ее в JSON формате.
            
            Извлеки следующую информацию:
            1. Для кого ищем досуг (роль члена семьи и возраст, если указан)
            2. Дату или день недели (если указаны)
            3. Предпочтения по досугу
            4. Ограничения или противопоказания
            5. Определи, нужен ли автоматический подбор свободного времени
            
            Если какой-то из параметров не указан, оставь его значение null или пустой массив.
            
            Верни ответ строго в следующем JSON формате:
            {
              "familyMember": {
                "role": "string", // роль, например "дочь", "сын", "жена" и т.д.
                "age": number // возраст, если указан
              },
              "date": "string", // конкретная дата в формате "YYYY-MM-DD" если указана
              "dayOfWeek": "string", // день недели на русском, если указан
              "preferences": ["string"], // список предпочтений
              "restrictions": ["string"], // список ограничений
              "needsTimeSlotSelection": boolean // true, если пользователь просит подобрать время автоматически
            }
            
            Важно: верни ТОЛЬКО JSON без пояснений.
        """
    }

    /**
     * Extracts activity request information from the user message
     * 
     * @param userMessage User's message text
     * @return Structured ActivityRequestData object
     */
    fun extractActivityRequest(userMessage: String): ActivityRequestData {
        val systemPrompt = SystemPromptTemplate.builder()
            .template(EXTRACTION_PROMPT)
            .build()
            .create()
        
        val response = chatClient
            .prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call()
            .content()
        
        return try {
            objectMapper.readValue<ActivityRequestData>(response)
        } catch (e: Exception) {
            // Log the error and return empty data
            println("Error parsing JSON response: $e")
            println("Response content: $response")
            ActivityRequestData()
        }
    }
    
    /**
     * Determines if the current activity request needs more information
     * 
     * @param request The current activity request data
     * @return A pair of (needsMoreInfo, missingFields)
     */
    fun needsMoreInformation(request: ActivityRequestData): Pair<Boolean, List<String>> {
        val missingFields = mutableListOf<String>()
        
        // Check for missing required fields
        if (request.familyMember == null || request.familyMember.role == null) {
            missingFields.add("familyMember")
        }
        
        if (request.date == null && request.dayOfWeek == null && !request.needsTimeSlotSelection) {
            missingFields.add("dateOrTimeSlot")
        }
        
        if (request.preferences.isEmpty()) {
            missingFields.add("preferences")
        }
        
        return Pair(missingFields.isNotEmpty(), missingFields)
    }
    
    /**
     * Generates a follow-up question to gather missing information
     * 
     * @param missingFields List of missing information fields
     * @return A follow-up question for the user
     */
    fun generateFollowUpQuestion(missingFields: List<String>): String {
        val followUpPrompt = """
            Пользователю нужно задать уточняющие вопросы о планировании досуга.
            Ему нужно уточнить следующие детали: ${missingFields.joinToString(", ")}.
            
            Сформулируй вежливый и краткий вопрос на русском языке, чтобы получить недостающую информацию.
            Не упоминай термин "JSON" или технические детали.
        """.trimIndent()
        
        return chatClient
            .prompt()
            .user(followUpPrompt)
            .call()
            .content()
    }
} 