package rut.uvp.family.services

import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRecommendation
import rut.uvp.family.models.ActivitySearchQuery
import rut.uvp.family.models.Activity
import org.slf4j.LoggerFactory

/**
 * Сервис для парсинга и поиска активностей.
 * Этот сервис теперь делегирует основную функциональность KudaGoApiService
 */
@Service
class ParserService(
    private val kudaGoApiService: KudaGoApiService
) {
    private val logger = LoggerFactory.getLogger(ParserService::class.java)
    
    /**
     * Поиск активностей на основе поискового запроса
     */
    fun searchActivities(searchQuery: ActivitySearchQuery): List<ActivityRecommendation> {
        logger.info("Searching activities with query: ${searchQuery.searchQuery}")
        
        // Используем KudaGoApiService для поиска мероприятий
        val activities = kudaGoApiService.searchEvents(
            keywords = searchQuery.searchQuery,
            city = searchQuery.filters["город"] ?: "msk",
            isFree = searchQuery.filters["бесплатно"]?.toBoolean(),
            categories = searchQuery.filters["категория"],
            dateFrom = searchQuery.filters["дата"]
        )
        
        // Конвертируем в рекомендации
        return activities.map { activity ->
            convertToRecommendation(activity)
        }
    }
    
    /**
     * Конвертация Activity в ActivityRecommendation
     */
    private fun convertToRecommendation(activity: Activity): ActivityRecommendation {
        return ActivityRecommendation(
            title = activity.title,
            description = activity.description,
            imageUrl = activity.imageUrl,
            date = activity.date,
            time = activity.time,
            location = activity.location,
            price = activity.price,
            ageRestriction = activity.ageRestriction,
            category = activity.category,
            url = activity.link
        )
    }
} 