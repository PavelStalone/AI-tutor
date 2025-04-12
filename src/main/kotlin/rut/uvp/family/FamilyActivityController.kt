package rut.uvp.family

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.FamilyActivityResponse
import rut.uvp.family.services.ConversationFlowService
import rut.uvp.family.services.DateSelectionService
import rut.uvp.family.services.ParserService
import rut.uvp.family.services.SearchQueryService
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Controller for handling family activity recommendation requests
 */
@RestController
@RequestMapping("family-activity")
class FamilyActivityController(
    private val conversationFlowService: ConversationFlowService,
    private val dateSelectionService: DateSelectionService,
    private val searchQueryService: SearchQueryService,
    private val parserService: ParserService,
    private val objectMapper: ObjectMapper
) {
    /**
     * Process a user query for family activity recommendations
     * 
     * @param request The user message request
     * @return A stream of response messages or the final JSON result
     */
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun processActivityRequest(@RequestBody request: MessageRequest): FamilyActivityResponse {
        // Step 1: Extract information from the user message
        val activityRequest = conversationFlowService.extractActivityRequest(request.message)
        
        // Step 2: Check if more information is needed
        val (needsMoreInfo, missingFields) = conversationFlowService.needsMoreInformation(activityRequest)
        if (needsMoreInfo) {
            // In a real application, we would return a message asking for more information
            // For simplicity, we'll continue with whatever information we have
            println("Missing information: $missingFields")
        }
        
        // Step 3: Auto-select time slot if needed
        val selectedTimeSlot = if (activityRequest.needsTimeSlotSelection) {
            dateSelectionService.selectTimeSlot(activityRequest)
        } else {
            null
        }
        
        // Step 4: Generate search query
        val searchQuery = searchQueryService.generateSearchQuery(activityRequest, selectedTimeSlot)
            ?: return FamilyActivityResponse(activityRequest) // Return empty response if query generation failed
        
        // Step 5: Search for activities
        val activities = parserService.searchActivities(searchQuery)
        
        // Step 6: Return the results as a structured response
        return FamilyActivityResponse(
            request = activityRequest,
            selectedTimeSlot = selectedTimeSlot,
            activities = activities
        )
    }
    
    /**
     * Process a user query for family activity recommendations with streamed responses
     * This endpoint provides a more interactive experience by streaming responses
     *
     * @param request The user message request
     * @return A stream of response messages
     */
    @PostMapping(path = ["/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun processActivityRequestStream(@RequestBody request: MessageRequest): Flux<String> {
        return Mono.fromCallable { processActivityRequest(request) }
            .map { response -> objectMapper.writeValueAsString(response) }
            .flux()
    }
    
    /**
     * Data class for representing a message request
     */
    data class MessageRequest(
        val message: String
    )
} 