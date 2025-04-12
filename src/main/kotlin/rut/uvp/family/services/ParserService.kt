package rut.uvp.family.services

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRecommendation
import rut.uvp.family.models.ActivitySearchQuery
import rut.uvp.family.models.KudaGoSearchParams
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
class ParserService(
    private val chatClient: ChatClient,
    private val objectMapper: ObjectMapper,
    private val kudaGoApiService: KudaGoApiService
) {
    companion object {
        private const val PARSE_RESULT_PROMPT = """
            Ты - парсер мероприятий для семейного досуга. Твоя задача - извлечь структурированную информацию о мероприятиях из HTML-контента.
            
            Поисковый запрос:
            {{searchQuery}}
            
            HTML-контент или результаты поиска:
            {{htmlContent}}
            
            Извлеки информацию о мероприятиях, включая следующие поля (если они присутствуют):
            - Название (title) - обязательное поле
            - Описание (description)
            - URL изображения (imageUrl)
            - Дата проведения (date)
            - Время проведения (time)
            - Место проведения (location)
            - Цена (price)
            - Возрастное ограничение (ageRestriction)
            - Категория (category)
            - URL мероприятия (url)
            
            Верни результат ТОЛЬКО в формате JSON-массива объектов:
            [
              {
                "title": "string",
                "description": "string",
                "imageUrl": "string",
                "date": "string",
                "time": "string",
                "location": "string",
                "price": "string",
                "ageRestriction": "string",
                "category": "string",
                "url": "string"
              }
            ]
            
            Важно:
            1. Верни не более 5 мероприятий
            2. Не добавляй поля, которых нет в результатах
            3. Все поля, кроме title, могут быть null, если информация отсутствует
            4. Не выдумывай информацию, которой нет в исходных данных
        """
        
        private const val MOCK_DATA_FILE = "mock_activities.json"
        
        // Маппинг категорий для KudaGo
        private val CATEGORY_MAPPING = mapOf(
            "концерт" to "concert",
            "выставка" to "exhibition",
            "спектакль" to "theater",
            "кино" to "cinema",
            "фестиваль" to "festival",
            "вечеринка" to "party",
            "шоу" to "show",
            "ярмарка" to "fair",
            "для детей" to "kids",
            "мастер-класс" to "masterclass",
            "экскурсия" to "tour",
            "квест" to "quest",
            "образование" to "education",
            "музей" to "museum",
            "активный отдых" to "active",
            "спорт" to "sport"
        )
    }
    
    /**
     * Searches for activities based on the search query
     *
     * @param searchQuery The search query and filters
     * @return A list of activity recommendations
     */
    fun searchActivities(searchQuery: ActivitySearchQuery): List<ActivityRecommendation> {
        // Попытка использовать KudaGo API
        val kudaGoEvents = searchKudaGoEvents(searchQuery)
        if (kudaGoEvents.isNotEmpty()) {
            return kudaGoEvents
        }
        
        // Если KudaGo не вернул результатов, используем моковые данные
        return getMockActivities(searchQuery)
    }
    
    /**
     * Поиск мероприятий через API KudaGo
     * 
     * @param searchQuery Поисковый запрос
     * @return Список рекомендаций мероприятий
     */
    private fun searchKudaGoEvents(searchQuery: ActivitySearchQuery): List<ActivityRecommendation> {
        try {
            // Конвертируем наш поисковый запрос в параметры для KudaGo API
            val params = convertToKudaGoParams(searchQuery)
            
            // Выполняем запрос к API
            val events = kudaGoApiService.searchEvents(params)
            
            // Конвертируем полученные события в наш формат
            return if (events.isNotEmpty()) {
                kudaGoApiService.convertToRecommendations(events)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error searching KudaGo events: ${e.message}")
            return emptyList()
        }
    }
    
    /**
     * Конвертация нашего поискового запроса в параметры для KudaGo
     * 
     * @param searchQuery Наш поисковый запрос
     * @return Параметры поиска для KudaGo API
     */
    private fun convertToKudaGoParams(searchQuery: ActivitySearchQuery): KudaGoSearchParams {
        // Определение местоположения (по умолчанию - Москва)
        val location = "msk" // По умолчанию Москва
        
        // Выбор категорий на основе поискового запроса
        val categories = mutableListOf<String>()
        val searchQueryLower = searchQuery.searchQuery.lowercase()
        
        // Определяем категории на основе запроса и фильтров
        CATEGORY_MAPPING.forEach { (keyword, category) ->
            if (searchQueryLower.contains(keyword)) {
                categories.add(category)
            }
        }
        
        // Добавляем категории из фильтров
        searchQuery.filters["категория"]?.let { category ->
            CATEGORY_MAPPING[category.lowercase()]?.let { categories.add(it) }
        }
        
        // Определение временного диапазона
        val now = Instant.now()
        val startDate = now
        val endDate = now.plus(30, ChronoUnit.DAYS) // По умолчанию ищем на 30 дней вперед
        
        // Если в запросе есть конкретная дата
        searchQuery.filters["дата"]?.let { dateStr ->
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val date = LocalDate.parse(dateStr, formatter)
                val instant = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val nextDay = instant.plus(1, ChronoUnit.DAYS)
                return KudaGoSearchParams(
                    location = location,
                    categories = categories,
                    actualSince = instant,
                    actualUntil = nextDay,
                    query = searchQuery.searchQuery,
                    ageRestriction = searchQuery.filters["возраст"]
                )
            } catch (e: Exception) {
                println("Error parsing date: ${e.message}")
            }
        }
        
        // Если в запросе есть день недели
        searchQuery.filters["день_недели"]?.let { dayOfWeek ->
            // Логика определения следующего указанного дня недели
            // (здесь можно добавить сложную логику, но для простоты оставляем временной диапазон по умолчанию)
        }
        
        return KudaGoSearchParams(
            location = location,
            categories = categories,
            actualSince = startDate,
            actualUntil = endDate,
            query = searchQuery.searchQuery,
            ageRestriction = searchQuery.filters["возраст"]
        )
    }
    
    /**
     * Fetches HTML content from a target URL based on the search query
     *
     * @param searchQuery The search query and filters
     * @return The HTML content as a string
     */
    private fun fetchHtmlContent(searchQuery: ActivitySearchQuery): String {
        try {
            // Build search URL
            val baseUrl = "https://example.com/events"
            val query = URLEncoder.encode(searchQuery.searchQuery, StandardCharsets.UTF_8.toString())
            val url = "$baseUrl?q=$query"
            
            // Add filters if available
            val fullUrl = searchQuery.filters.entries.fold(url) { acc, (key, value) ->
                val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                "$acc&$key=$encodedValue"
            }
            
            // Fetch HTML content
            val document: Document = Jsoup.connect(fullUrl)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get()
            
            return document.html()
        } catch (e: Exception) {
            println("Error fetching HTML content: $e")
            return ""
        }
    }
    
    /**
     * Parses activities from HTML content using LLM
     *
     * @param htmlContent The HTML content to parse
     * @param searchQuery The original search query for context
     * @return A list of parsed activity recommendations
     */
    private fun parseActivitiesFromHtml(htmlContent: String, searchQuery: ActivitySearchQuery): List<ActivityRecommendation> {
        if (htmlContent.isBlank()) return emptyList()
        
        // Build the prompt with HTML content and search query
        val systemPrompt = SystemPromptTemplate.builder()
            .template(PARSE_RESULT_PROMPT)
            .parameter("searchQuery", objectMapper.writeValueAsString(searchQuery))
            .parameter("htmlContent", htmlContent)
            .build()
            .create()
        
        // Parse the HTML content using the LLM
        val response = chatClient
            .prompt()
            .system(systemPrompt)
            .call()
            .content()
        
        return try {
            objectMapper.readValue<List<ActivityRecommendation>>(response)
        } catch (e: Exception) {
            println("Error parsing activities from HTML: $e")
            println("Response content: $response")
            emptyList()
        }
    }
    
    /**
     * Gets mock activity data for testing/demonstration
     *
     * @param searchQuery The search query to filter mock data
     * @return A list of mock activities
     */
    private fun getMockActivities(searchQuery: ActivitySearchQuery): List<ActivityRecommendation> {
        try {
            // Read mock data from file
            val resource = ClassPathResource(MOCK_DATA_FILE)
            val reader = BufferedReader(InputStreamReader(resource.inputStream))
            val jsonContent = reader.readText()
            
            // Parse mock data
            val activities = objectMapper.readValue<List<ActivityRecommendation>>(jsonContent)
            
            // Filter activities based on search query (simple text matching for demo)
            return activities.filter { activity ->
                val matchesQuery = activity.title.contains(searchQuery.searchQuery, ignoreCase = true) ||
                        (activity.description?.contains(searchQuery.searchQuery, ignoreCase = true) ?: false)
                
                // Filter by age if specified
                val ageFilter = searchQuery.filters["возраст"]
                val matchesAge = if (ageFilter != null) {
                    activity.ageRestriction?.contains(ageFilter, ignoreCase = true) ?: false
                } else {
                    true
                }
                
                // Filter by category if specified
                val categoryFilter = searchQuery.filters["категория"] ?: searchQuery.filters["тип_мероприятия"]
                val matchesCategory = if (categoryFilter != null) {
                    activity.category?.contains(categoryFilter, ignoreCase = true) ?: false
                } else {
                    true
                }
                
                matchesQuery && matchesAge && matchesCategory
            }.take(5)  // Limit to 5 results
        } catch (e: Exception) {
            println("Error reading mock activities: $e")
            
            // If mock file not found, create some hardcoded samples
            return createHardcodedSamples(searchQuery)
        }
    }
    
    /**
     * Creates hardcoded sample activities if mock file is not available
     *
     * @param searchQuery The search query for context
     * @return A list of hardcoded sample activities
     */
    private fun createHardcodedSamples(searchQuery: ActivitySearchQuery): List<ActivityRecommendation> {
        val childAge = searchQuery.filters["возраст"] ?: "6"
        val isChildActivity = searchQuery.searchQuery.contains("ребенком") || 
                             searchQuery.searchQuery.contains("детьми") ||
                             searchQuery.searchQuery.contains("дочкой") ||
                             searchQuery.searchQuery.contains("сыном")
        
        return if (isChildActivity) {
            listOf(
                ActivityRecommendation(
                    title = "Мастер-класс по рисованию для детей",
                    description = "Увлекательный мастер-класс по рисованию для детей от 5 до 10 лет. Профессиональные художники научат вашего ребенка основам живописи.",
                    imageUrl = "https://example.com/images/drawing-class.jpg",
                    date = "2023-10-01",
                    time = "12:00-14:00",
                    location = "Детский центр 'Радуга', ул. Пушкина, 10",
                    price = "1000 руб",
                    ageRestriction = "5-10 лет",
                    category = "Мастер-класс",
                    url = "https://example.com/events/drawing-class"
                ),
                ActivityRecommendation(
                    title = "Интерактивный научный музей 'Экспериментариум'",
                    description = "Музей, где дети могут в игровой форме познакомиться с законами физики, химии и биологии через интерактивные экспонаты.",
                    imageUrl = "https://example.com/images/experimentarium.jpg",
                    date = "2023-10-01",
                    time = "10:00-19:00",
                    location = "ТЦ 'Гранд', 3 этаж",
                    price = "700 руб",
                    ageRestriction = "0+",
                    category = "Музей",
                    url = "https://example.com/events/experimentarium"
                ),
                ActivityRecommendation(
                    title = "Спектакль 'Приключения Чиполлино'",
                    description = "Детский музыкальный спектакль по мотивам сказки Джанни Родари.",
                    imageUrl = "https://example.com/images/cipollino.jpg",
                    date = "2023-10-01",
                    time = "15:00-16:30",
                    location = "Детский театр кукол",
                    price = "800 руб",
                    ageRestriction = "3+",
                    category = "Театр",
                    url = "https://example.com/events/cipollino"
                )
            )
        } else {
            listOf(
                ActivityRecommendation(
                    title = "Выставка современного искусства",
                    description = "Экспозиция работ современных художников из России и Европы.",
                    imageUrl = "https://example.com/images/art-exhibition.jpg",
                    date = "2023-10-01",
                    time = "11:00-20:00",
                    location = "Центр современного искусства",
                    price = "500 руб",
                    ageRestriction = "12+",
                    category = "Выставка",
                    url = "https://example.com/events/art-exhibition"
                ),
                ActivityRecommendation(
                    title = "Концерт классической музыки",
                    description = "Произведения Моцарта и Бетховена в исполнении симфонического оркестра.",
                    imageUrl = "https://example.com/images/classic-music.jpg",
                    date = "2023-10-01",
                    time = "19:00-21:00",
                    location = "Филармония",
                    price = "1500 руб",
                    ageRestriction = "6+",
                    category = "Концерт",
                    url = "https://example.com/events/classic-music"
                )
            )
        }
    }
} 