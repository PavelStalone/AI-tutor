package rut.uvp.family.services

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.FamilyMember
import rut.uvp.family.models.FamilyMemberData
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Расширенная версия сервиса ConversationFlowService, которая использует RAG для улучшения анализа запросов
 */
@Service
@Primary // Этот сервис будет использоваться вместо стандартного ConversationFlowService
class EnhancedConversationFlowService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    private val vectorStore: VectorStore,
    private val familyService: FamilyService
) {
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
            - activityDuration: String? - продолжительность активности ("короткая", "полдня", "весь день")
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
     *
     * @param userMessage сообщение пользователя
     * @return структурированные данные о запросе активности или null, если данные извлечь не удалось
     */
    fun extractActivityRequest(userMessage: String): ActivityRequestData? {
        val messages = listOf(
            SystemMessage(EXTRACT_SYSTEM_PROMPT),
            UserMessage(userMessage)
        )
        
        val prompt = Prompt(messages)
        val response = chatClient.call(prompt)
        
        return try {
            val jsonResponse = response.result.output.content
            objectMapper.readValue<ActivityRequestData?>(jsonResponse)
        } catch (e: Exception) {
            println("Error parsing activity request: ${e.message}")
            null
        }
    }
    
    /**
     * Проверяет, нужна ли дополнительная информация для поиска активности
     *
     * @param activityRequest данные о запросе активности
     * @return пара (нужна ли информация, список недостающих полей)
     */
    fun needsMoreInformation(activityRequest: ActivityRequestData?): Pair<Boolean, List<String>> {
        if (activityRequest == null) {
            return Pair(true, listOf("activityType", "familyMember"))
        }
        
        val missingFields = mutableListOf<String>()
        
        if (activityRequest.activityType == null) {
            missingFields.add("activityType")
        }
        
        if (activityRequest.familyMember == null || activityRequest.familyMember.role == null) {
            missingFields.add("familyMember")
        } else if (activityRequest.familyMember.age == null) {
            missingFields.add("familyMemberAge")
        }
        
        if (activityRequest.preferredDate == null) {
            missingFields.add("preferredDate")
        }
        
        return Pair(missingFields.isNotEmpty(), missingFields)
    }
    
    /**
     * Генерирует уточняющий вопрос для получения недостающей информации
     *
     * @param missingFields список недостающих полей
     * @param familyMemberRole роль члена семьи (если известна)
     * @return уточняющий вопрос
     */
    fun generateFollowUpQuestion(missingFields: List<String>, familyMemberRole: String?): String {
        val model = mapOf(
            "missingFields" to missingFields.joinToString("\n"),
            "familyMemberRole" to (familyMemberRole ?: "неизвестно")
        )
        
        val systemPrompt = SystemPromptTemplate(FOLLOW_UP_SYSTEM_PROMPT).create(model)
        val prompt = Prompt(listOf(systemPrompt, UserMessage("Сформируй уточняющий вопрос")))
        
        return chatClient.call(prompt).result.output.content
    }
    
    /**
     * Обогащает запрос информацией из векторного хранилища
     *
     * @param activityRequest данные о запросе активности
     * @return обогащенные данные о запросе активности
     */
    fun enrichRequestWithContextualData(activityRequest: ActivityRequestData): ActivityRequestData {
        val enriched = activityRequest.copy()
        
        // Если есть информация о члене семьи, ищем дополнительный контекст
        activityRequest.familyMember?.role?.let { role ->
            val familyContext = retrieveFamilyContext(role)
            if (familyContext.isNotEmpty()) {
                // Если возраст не указан, но есть в контексте - добавляем
                if (enriched.familyMember?.age == null) {
                    val ageRegex = "\\b(?:возраст|лет|года|год)\\s*[:=]\\s*(\\d+)\\b".toRegex()
                    val match = ageRegex.find(familyContext)
                    match?.groupValues?.get(1)?.toIntOrNull()?.let { age ->
                        enriched.familyMember = enriched.familyMember?.copy(age = age)
                    }
                }
                
                // Если есть особые интересы в контексте - добавляем
                val interestsRegex = "\\b(?:интересы|увлечения|хобби)\\s*[:=]\\s*([^.]+)".toRegex()
                val interestsMatch = interestsRegex.find(familyContext)
                interestsMatch?.groupValues?.get(1)?.split(",")?.map { it.trim() }?.let { interests ->
                    if (interests.isNotEmpty()) {
                        enriched.specialRequirements = (enriched.specialRequirements ?: emptyList()) + interests
                    }
                }
            }
        }
        
        return enriched
    }
    
    /**
     * Получает контекст о члене семьи из векторного хранилища
     *
     * @param role роль члена семьи
     * @return контекст о члене семьи
     */
    private fun retrieveFamilyContext(role: String): String {
        val searchResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("информация о $role")
                .filter("type:family_member")
                .topK(1)
                .build()
        )
        
        return searchResults.firstOrNull()?.text ?: ""
    }
} 