package rut.uvp.family.services

import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.CacheEvict
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.util.UriComponentsBuilder
import rut.uvp.family.models.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.time.format.DateTimeFormatter

/**
 * Сервис для работы с API KudaGo
 */
@Service
class KudaGoApiService(
    private val restTemplate: RestTemplate
) {
    private val logger = LoggerFactory.getLogger(KudaGoApiService::class.java)
    
    companion object {
        private const val API_BASE_URL = "https://kudago.com/public-api/v1.4"
        private const val LOCATIONS_ENDPOINT = "/locations"
        private const val CATEGORIES_ENDPOINT = "/event-categories"
        private const val EVENTS_ENDPOINT = "/events"
        
        // Кэши
        private const val LOCATIONS_CACHE = "kudago-locations"
        private const val CATEGORIES_CACHE = "kudago-categories"
        private const val EVENTS_CACHE = "kudago-events"
        private const val EVENT_DETAILS_CACHE = "kudago-event-details"
    }

    /**
     * Получение списка доступных городов
     */
    @Cacheable(LOCATIONS_CACHE)
    fun getLocations(): List<KudaGoLocation> {
        val url = "$API_BASE_URL$LOCATIONS_ENDPOINT"
        logger.info("KudaGo API REQUEST: GET $url")
        
        return try {
            val response = restTemplate.getForObject(url, KudaGoLocationResponse::class.java)
            logger.debug("KudaGo API response received: ${response?.results?.size ?: 0} locations")
            response?.results ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching locations: ${e.message}")
            emptyList()
        }
    }

    /**
     * Получение списка категорий мероприятий
     */
    @Cacheable(CATEGORIES_CACHE)
    fun getCategories(): List<KudaGoCategory> {
        val url = "$API_BASE_URL$CATEGORIES_ENDPOINT"
        logger.info("KudaGo API REQUEST: GET $url")
        
        return try {
            val response = restTemplate.getForObject(url, KudaGoCategoryResponse::class.java)
            logger.debug("KudaGo API response received: ${response?.results?.size ?: 0} categories")
            response?.results ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error fetching categories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Поиск мероприятий по параметрам
     */
    @Cacheable(value = [EVENTS_CACHE], key = "#params.toString()")
    fun searchEvents(params: KudaGoSearchParams): List<KudaGoEvent> {
        logger.info("Searching events with params: $params")
        
        return try {
            val urlBuilder = UriComponentsBuilder.fromHttpUrl("$API_BASE_URL$EVENTS_ENDPOINT")
            
            params.location?.let { urlBuilder.queryParam("location", it) }
            if (params.categories.isNotEmpty()) {
                urlBuilder.queryParam("categories", params.categories.joinToString(","))
            }
            params.actualSince?.let { urlBuilder.queryParam("actual_since", it.epochSecond) }
            params.actualUntil?.let { urlBuilder.queryParam("actual_until", it.epochSecond) }
            params.ageRestriction?.let { urlBuilder.queryParam("age_restriction", it) }
            params.isFree?.let { urlBuilder.queryParam("is_free", it) }
            params.query?.let { urlBuilder.queryParam("q", it) }
            urlBuilder.queryParam("page_size", params.pageSize)
            urlBuilder.queryParam("page", params.page)
            
            val url = urlBuilder.build().toUriString()
            logger.info("KudaGo API REQUEST: GET $url")
            
            val response = restTemplate.getForObject(url, KudaGoEventResponse::class.java)
            logger.debug("KudaGo API response received: ${response?.results?.size ?: 0} events")
            response?.results ?: emptyList()
        } catch (e: Exception) {
            logger.error("Error searching events: ${e.message}")
            emptyList()
        }
    }

    /**
     * Получение детальной информации о мероприятии
     */
    @Cacheable(value = [EVENT_DETAILS_CACHE], key = "#eventId")
    fun getEventDetails(eventId: Int): KudaGoEvent? {
        val url = "$API_BASE_URL$EVENTS_ENDPOINT/$eventId"
        logger.info("KudaGo API REQUEST: GET $url")
        
        return try {
            val response = restTemplate.getForObject(url, KudaGoEvent::class.java)
            logger.debug("KudaGo API response received for event ID: $eventId")
            response
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                logger.warn("Event with ID $eventId not found")
            } else {
                logger.error("Error fetching event details: ${e.message}")
            }
            null
        } catch (e: Exception) {
            logger.error("Error fetching event details: ${e.message}")
            null
        }
    }

    /**
     * Форматирование информации о дате и времени
     */
    private fun formatDateTimeInfo(dates: List<KudaGoDateRange>): Pair<String?, String?> {
        if (dates.isEmpty()) return Pair(null, null)
        
        val dateRange = dates.first()
        
        val formattedDate = if (dateRange.startDate != null) {
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            val startDate = LocalDate.parse(dateRange.startDate)
            
            if (dateRange.endDate != null && dateRange.endDate != dateRange.startDate) {
                val endDate = LocalDate.parse(dateRange.endDate)
                "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"
            } else {
                startDate.format(dateFormatter)
            }
        } else {
            val startInstant = Instant.ofEpochSecond(dateRange.start)
            val startDate = LocalDate.ofInstant(startInstant, ZoneId.systemDefault())
            val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
            
            if (dateRange.end != null && dateRange.end != dateRange.start) {
                val endInstant = Instant.ofEpochSecond(dateRange.end)
                val endDate = LocalDate.ofInstant(endInstant, ZoneId.systemDefault())
                "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}"
            } else {
                startDate.format(dateFormatter)
            }
        }
        
        val formattedTime = if (dateRange.startTime != null) {
            if (dateRange.endTime != null && dateRange.endTime != dateRange.startTime) {
                "${dateRange.startTime.substring(0, 5)} - ${dateRange.endTime.substring(0, 5)}"
            } else {
                dateRange.startTime.substring(0, 5)
            }
        } else {
            null
        }
        
        return Pair(formattedDate, formattedTime)
    }

    /**
     * Форматирование информации о месте проведения
     */
    private fun formatLocation(place: KudaGoPlace?): String? {
        if (place == null) return null
        
        return if (place.address != null && place.title != null) {
            "${place.title}, ${place.address}"
        } else {
            place.title ?: place.address
        }
    }

    /**
     * Очистка всех кэшей (по расписанию каждый день в 4 утра)
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @CacheEvict(value = [LOCATIONS_CACHE, CATEGORIES_CACHE, EVENTS_CACHE, EVENT_DETAILS_CACHE], allEntries = true)
    fun clearCaches() {
        logger.info("Clearing all KudaGo API caches")
    }

    /**
     * Поиск мероприятий по параметрам
     */
    fun searchEvents(
        keywords: String,
        city: String = "msk",
        isFree: Boolean? = null,
        categories: String? = null,
        dateFrom: String? = null
    ): List<Activity> {
        logger.info("Search request: keywords='$keywords', city='$city', isFree=$isFree, categories='$categories', dateFrom='$dateFrom'")
        
        val params = KudaGoSearchParams(
            location = city,
            categories = categories?.split(",")?.map { it.trim() } ?: emptyList(),
            actualSince = dateFrom?.let { java.time.Instant.parse(it + "T00:00:00Z") },
            isFree = isFree,
            query = keywords
        )
        
        val events = searchEvents(params)
        logger.info("Found ${events.size} events from KudaGo API for '$keywords'")
        
        return events.map { event ->
            val dateTimeInfo = formatDateTimeInfo(event.dates)
            
            Activity(
                id = event.id.toString(),
                title = event.title,
                description = event.shortDescription ?: event.description ?: "",
                date = dateTimeInfo.first,
                time = dateTimeInfo.second,
                location = formatLocation(event.place),
                price = event.price,
                ageRestriction = event.ageRestriction,
                category = event.categories.firstOrNull()?.name,
                link = "https://kudago.com/msk/event/${event.slug}/",
                imageUrl = event.images.firstOrNull()?.image
            )
        }
    }

    /**
     * Конвертирует KudaGoEvent в модель ActivityRecommendation для передачи клиенту
     */
    fun convertToRecommendations(events: List<KudaGoEvent>): List<ActivityRecommendation> {
        return events.map { event ->
            val dateTimeInfo = formatDateTimeInfo(event.dates)
            
            ActivityRecommendation(
                title = event.title,
                description = event.shortDescription ?: event.description,
                imageUrl = event.images.firstOrNull()?.image,
                date = dateTimeInfo.first,
                time = dateTimeInfo.second,
                location = formatLocation(event.place),
                price = event.price,
                ageRestriction = event.ageRestriction,
                category = event.categories.firstOrNull()?.name,
                url = "https://kudago.com/msk/event/${event.slug}/"
            )
        }
    }
}
