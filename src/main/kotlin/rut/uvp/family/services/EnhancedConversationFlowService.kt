package rut.uvp.family.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import reactor.core.publisher.Mono

@Service
@Primary
class EnhancedConversationFlowService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(EnhancedConversationFlowService::class.java)

    companion object {
        private const val EXTRACT_SYSTEM_PROMPT = """
            Ты - помощник для анализа запросов о семейных мероприятиях и досуге.
            
            Твоя задача - извлечь структурированную информацию из запроса пользователя о поиске семейных активностей.
            
            ВАЖНО:
            - Анализируй запрос пользователя и определи все доступные данные о желаемой активности, возрасте ребенка, дате/времени и т.д.
            - Извлекай информацию о члене семьи. Типичные роли в семье: "мама", "папа", "сын", "дочь", "бабушка", "дедушка", "внук", "внучка".
            - По возможности извлекай возраст членов семьи - это поможет подобрать подходящие мероприятия.
            - Если пользователь не указал конкретную дату, но использовал слова типа "сегодня", "завтра", "в выходные", интерпретируй их соответствующим образом.
            - Для каждого поля, которое пользователь не указал явно, используй значение null.
            - Старайся извлечь как можно больше релевантной информации из запроса.
            
            ВЫХОДНЫЕ ДАННЫЕ:
            Ты должен вернуть JSON объект типа ActivityRequestData со следующими полями:
            - activityType: String? - тип активности (например, "музей", "парк", "кино", "концерт", "театр", "мастер-класс")
            - familyMember: FamilyMember? - информация о члене семьи (поля: role: String? - роль в семье, age: Int? - возраст)
            - preferredDate: String? - предпочтительная дата в формате ISO (YYYY-MM-DD)
            - needsTimeSlotSelection: Boolean - нужно ли предложить пользователю выбор временного слота
            - budgetConstraint: String? - ограничения по бюджету ("бесплатно", "недорого", "без ограничений")
            - locationPreference: String? - предпочтения по месту проведения (район, город)
            - specialRequirements: List<String> - особые требования или интересы
            
            Если ты не смог извлечь из запроса пользователя данные для поиска активности, верни null.
        """

        private const val FOLLOW_UP_SYSTEM_PROMPT = """
            Ты - помощник для поиска семейных мероприятий и досуга. 
            
            Твоя задача - сформировать вежливый и естественный вопрос, чтобы получить недостающую информацию от пользователя.
            
            У тебя есть список полей, для которых нужно запросить дополнительную информацию:
            {{missingFields}}
            
            Семейная роль пользователя или члена семьи, для которого ищется активность:
            {{familyMemberRole}}
            
            Сформируй один вопрос, который поможет получить всю недостающую информацию одновременно, но звучит естественно в разговоре.
            Обращайся к пользователю на "вы". Вопрос должен быть вежливым, но кратким.
            
            Примеры хороших вопросов:
            - "Не могли бы вы сказать, для какого возраста ребенка ищете активность и в какой день планируете?"
            - "Уточните, пожалуйста, какой тип активности интересует для вашего ребенка и каков его возраст?"
            - "В какой день и район города вам удобно организовать досуг для дочки?"
            
            Результат должен быть одним текстовым вопросом без дополнительных комментариев.
        """
    }

    /**
     * Извлекает структурированные данные о запросе активности из сообщения пользователя
     */
    suspend fun extractActivityRequest(userMessage: String): ActivityRequestData? = withContext(Dispatchers.IO) {
        logger.debug("Extracting activity request from message: $userMessage")

        try {
            val promptBuilder = chatClient.prompt()
                .system(EXTRACT_SYSTEM_PROMPT)
                .user(userMessage)

            val response = callAndExtractResponse(promptBuilder)

            logger.debug("AI response for activity extraction: $response")

            try {
                objectMapper.readValue(response, ActivityRequestData::class.java)
            } catch (e: Exception) {
                logger.error("JSON parsing error: ${e.message}, response: $response")
                null
            }
        } catch (e: Exception) {
            logger.error("Error parsing activity request: ${e.message}")
            null
        }
    }

    private suspend fun callAndExtractResponse(promptTemplate: ChatClient.ChatClientRequestSpec): String = withContext(Dispatchers.IO) {
        try {
            val result = Mono.fromCallable { promptTemplate.call() }.block()
            result?.content() ?: ""
        } catch (e: Exception) {
            logger.error("Error in ChatClient: ${e.message}")
            throw e
        }
    }

    fun needsMoreInformation(activityRequest: ActivityRequestData?): Pair<Boolean, List<String>> {
        if (activityRequest == null) {
            return Pair(true, listOf("activityType", "familyMember"))
        }

        val missingFields = mutableListOf<String>()

        if (activityRequest.activityType == null) {
            missingFields.add("activityType")
        }

        if (activityRequest.familyMember?.role == null) {
            missingFields.add("familyMember")
        } else if (activityRequest.familyMember.age == null) {
            missingFields.add("familyMemberAge")
        }

        if (activityRequest.preferredDate == null) {
            missingFields.add("preferredDate")
        }

        return Pair(missingFields.isNotEmpty(), missingFields)
    }

    suspend fun generateFollowUpQuestion(missingFields: List<String>, familyMemberRole: String?): String = withContext(Dispatchers.IO) {
        logger.debug("Generating follow-up question for missing fields: {}, role: {}", missingFields, familyMemberRole)

        val systemPromptText = FOLLOW_UP_SYSTEM_PROMPT
            .replace("{{missingFields}}", missingFields.joinToString("\n"))
            .replace("{{familyMemberRole}}", familyMemberRole ?: "неизвестно")

        try {
            val promptBuilder = chatClient.prompt()
                .system(systemPromptText)
                .user("Сформируй уточняющий вопрос")

            val response = callAndExtractResponse(promptBuilder)
            
            logger.debug("AI response for follow-up question: $response")
            response
        } catch (e: Exception) {
            logger.error("Error generating follow-up question: ${e.message}")
            "Подскажите, пожалуйста, больше информации о планируемом досуге."
        }
    }
}
