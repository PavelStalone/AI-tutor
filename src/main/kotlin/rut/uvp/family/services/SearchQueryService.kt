package rut.uvp.family.services

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.ActivitySearchQuery
import rut.uvp.family.models.SelectedTimeSlot
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class SearchQueryService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    private val vectorStore: VectorStore
) {
    companion object {
        private const val QUERY_GENERATION_PROMPT = """
            Ты - помощник для генерации поисковых запросов мероприятий для семейного досуга.
            Твоя задача - создать поисковый запрос и набор фильтров для поиска подходящих мероприятий.
            
            Информация о запросе:
            {{request}}
            
            {{#timeSlot}}
            Выбранный временной слот:
            {{timeSlot}}
            {{/timeSlot}}
            
            {{#familyContext}}
            Информация о члене семьи:
            {{familyContext}}
            {{/familyContext}}
            
            {{#calendarContext}}
            Информация о календаре:
            {{calendarContext}}
            {{/calendarContext}}
            
            Создай поисковый запрос и набор фильтров, учитывая:
            1. Для кого ищем мероприятие (возраст и роль члена семьи)
            2. Предпочтения по досугу
            3. Ограничения
            4. Выбранную дату и время (если указаны)
            5. Город (по умолчанию - Москва)
            6. Информацию из контекста о члене семьи и его календаре
            
            Верни результат ТОЛЬКО в следующем JSON формате:
            {
              "searchQuery": "string", // основной поисковый запрос
              "filters": { // набор фильтров в формате ключ-значение
                "key1": "value1",
                "key2": "value2"
              }
            }
            
            Возможные фильтры: 
            - "возраст" - возрастное ограничение (например, "0+", "6+", "12+", "18+")
            - "тип_мероприятия" или "категория" - тип мероприятия (например, "выставка", "концерт", "мастер-класс")
            - "дата" - дата в формате "YYYY-MM-DD"
            - "день_недели" - день недели на русском языке
            - "город" - город проведения мероприятия (по умолчанию "москва")
            - "бесплатно" - флаг бесплатного мероприятия (true/false)
        """
        
        // Маппинг городов для KudaGo
        private val CITY_MAPPING = mapOf(
            "москва" to "msk",
            "санкт-петербург" to "spb",
            "петербург" to "spb",
            "спб" to "spb",
            "новосибирск" to "nsk",
            "екатеринбург" to "ekb",
            "нижний новгород" to "nnv",
            "казань" to "kzn",
            "челябинск" to "chlb",
            "красноярск" to "krasnoyarsk",
            "калининград" to "kgd",
            "сочи" to "sochi"
        )
    }
    
    /**
     * Generates a search query based on the activity request data
     *
     * @param request The activity request data
     * @param selectedTimeSlot Optional selected time slot
     * @return The generated search query or null if generation failed
     */
    fun generateSearchQuery(
        request: ActivityRequestData, 
        selectedTimeSlot: SelectedTimeSlot? = null
    ): ActivitySearchQuery? {
        // Получаем дополнительный контекст из RAG если есть информация о члене семьи
        val familyContext = if (request.familyMember?.role != null) {
            retrieveFamilyContext(request.familyMember.role)
        } else {
            null
        }
        
        // Получаем контекст о календаре из RAG если есть информация о члене семьи
        val calendarContext = if (request.familyMember?.role != null) {
            retrieveCalendarContext(request.familyMember.role)
        } else {
            null
        }
        
        // Build the prompt with request data and time slot if available
        val promptBuilder = SystemPromptTemplate.builder()
            .template(QUERY_GENERATION_PROMPT)
            .parameter("request", objectMapper.writeValueAsString(request))
        
        // Add time slot information if available
        if (selectedTimeSlot != null) {
            promptBuilder.parameter("timeSlot", objectMapper.writeValueAsString(selectedTimeSlot))
        }
        
        // Add family context if available
        if (familyContext != null) {
            promptBuilder.parameter("familyContext", familyContext)
        }
        
        // Add calendar context if available
        if (calendarContext != null) {
            promptBuilder.parameter("calendarContext", calendarContext)
        }
        
        val systemPrompt = promptBuilder.build().create()
        
        // Generate the search query using the LLM
        val response = chatClient
            .prompt()
            .system(systemPrompt)
            .call()
            .content()
        
        val query = try {
            objectMapper.readValue<ActivitySearchQuery>(response)
        } catch (e: Exception) {
            println("Error parsing search query generation: $e")
            println("Response content: $response")
            null
        } ?: return null
        
        // Нормализация фильтров
        normalizeFilters(query)
        
        return query
    }
    
    /**
     * Нормализует фильтры для поисковых запросов
     * 
     * @param query Поисковый запрос
     */
    private fun normalizeFilters(query: ActivitySearchQuery) {
        val filters = query.filters.toMutableMap()
        
        // Нормализация города
        filters["город"]?.let { cityName ->
            val normalizedCity = CITY_MAPPING[cityName.lowercase()] ?: "msk"
            filters["город"] = normalizedCity
        }
        
        // Добавить другие нормализации по необходимости
        
        // Обновляем фильтры в запросе
        query.filters.clear()
        query.filters.putAll(filters)
    }
    
    /**
     * Получает контекст о семье из векторного хранилища
     * 
     * @param query Запрос для поиска контекста
     * @return Контекст о семье в виде строки
     */
    private fun retrieveFamilyContext(query: String): String {
        val searchResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .filter("type:family_member OR type:member_preferences OR type:age_specific")
                .topK(3)
                .build()
        )
        
        return if (searchResults.isNullOrEmpty()) {
            "Информация о семье отсутствует."
        } else {
            searchResults.joinToString("\n\n") { it.text ?: "" }
        }
    }
    
    /**
     * Получает контекст о календаре из векторного хранилища
     * 
     * @param query Запрос для поиска контекста
     * @return Контекст о календаре в виде строки
     */
    private fun retrieveCalendarContext(query: String): String {
        val searchResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .filter("type:calendar_event OR type:availability")
                .topK(3)
                .build()
        )
        
        return if (searchResults.isNullOrEmpty()) {
            "Информация о календаре отсутствует."
        } else {
            searchResults.joinToString("\n\n") { it.text ?: "" }
        }
    }
    
    /**
     * Builds a search URL based on the generated search query and filters
     * This is an example implementation that can be customized based on the target search engine
     *
     * @param searchQuery The generated search query
     * @return A search URL string
     */
    fun buildSearchUrl(searchQuery: ActivitySearchQuery): String {
        val baseUrl = "https://example.com/search"
        val queryParam = "q=${searchQuery.searchQuery.replace(" ", "+")}"
        
        // Add filters as URL parameters
        val filterParams = searchQuery.filters.map { (key, value) -> 
            "$key=${value.replace(" ", "+")}" 
        }.joinToString("&")
        
        return if (filterParams.isNotEmpty()) {
            "$baseUrl?$queryParam&$filterParams"
        } else {
            "$baseUrl?$queryParam"
        }
    }
} 