package rut.uvp.family

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.ActivityRecommendation
import rut.uvp.family.models.FamilyActivityResponse
import rut.uvp.family.services.EnhancedConversationFlowService
import rut.uvp.family.services.TimeSlotService
import rut.uvp.family.services.SearchQueryService
import rut.uvp.family.services.KudaGoApiService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.web.bind.annotation.GetMapping

@RestController
@RequestMapping("family-activity")
class FamilyActivityController(
    private val conversationFlowService: EnhancedConversationFlowService,
    private val timeSlotService: TimeSlotService,
    private val searchQueryService: SearchQueryService,
    private val kudaGoApiService: KudaGoApiService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(FamilyActivityController::class.java)

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun processActivityRequest(@RequestBody request: MessageRequest): FamilyActivityResponse = withContext(Dispatchers.IO) {
        try {
            logger.debug("Processing activity request: {}", request.message)
            
            // Извлекаем данные о запросе активности
            val activityRequest = conversationFlowService.extractActivityRequest(request.message)
                ?: return@withContext FamilyActivityResponse(
                    request = ActivityRequestData(),
                    error = "Не удалось распознать запрос"
                )

            // Проверяем, нужна ли дополнительная информация
            val (needsMoreInfo, missingFields) = conversationFlowService.needsMoreInformation(activityRequest)
            if (needsMoreInfo) {
                logger.debug("Missing information: {}", missingFields)
                val followUpQuestion = conversationFlowService.generateFollowUpQuestion(missingFields, activityRequest.familyMember?.role)
                return@withContext FamilyActivityResponse(
                    request = activityRequest,
                    followUpQuestion = followUpQuestion
                )
            }

            // Выбор временного слота, если необходимо
            val selectedTimeSlot = if (activityRequest.needsTimeSlotSelection && activityRequest.preferredDate != null) {
                timeSlotService.generateTimeSlotsForDate(LocalDate.parse(activityRequest.preferredDate)).firstOrNull()
            } else {
                null
            }

            // Генерируем поисковый запрос
            val searchQuery = searchQueryService.generateSearchQuery(activityRequest, selectedTimeSlot)
                ?: return@withContext FamilyActivityResponse(
                    request = activityRequest,
                    error = "Не удалось сформировать поисковый запрос"
                )

            logger.debug("Generated search query: {}", searchQuery)

            // Ищем мероприятия через KudaGo API
            val activities = kudaGoApiService.searchEvents(
                keywords = searchQuery.searchQuery,
                city = searchQuery.filters["город"] ?: "msk",
                isFree = searchQuery.filters["бесплатно"]?.toBoolean(),
                categories = searchQuery.filters["категория"],
                dateFrom = searchQuery.filters["дата"] ?: activityRequest.preferredDate
            )

            if (activities.isEmpty()) {
                return@withContext FamilyActivityResponse(
                    request = activityRequest,
                    error = "Не найдено подходящих мероприятий"
                )
            }

            // Конвертируем в рекомендации
            val recommendations = activities.map { activity ->
                ActivityRecommendation(
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

            FamilyActivityResponse(
                request = activityRequest,
                selectedTimeSlot = selectedTimeSlot,
                activities = recommendations
            )
        } catch (e: Exception) {
            logger.error("Error processing activity request", e)
            FamilyActivityResponse(
                request = ActivityRequestData(),
                error = "Произошла ошибка при обработке запроса: ${e.message}"
            )
        }
    }

    @PostMapping(path = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun processActivityRequestStream(@RequestBody request: MessageRequest): Flow<String> = flow {
        try {
            val response = processActivityRequest(request)
            emit(objectMapper.writeValueAsString(response))
        } catch (e: Exception) {
            logger.error("Error in stream processing", e)
            emit(objectMapper.writeValueAsString(
                FamilyActivityResponse(
                    request = ActivityRequestData(),
                    error = "Ошибка потоковой обработки: ${e.message}"
                )
            ))
        }
    }

    @GetMapping
    fun getHomePage() : String {
        return "index.html"
    }

    data class MessageRequest(
        val message: String
    )
}
