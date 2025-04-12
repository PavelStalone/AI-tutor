package rut.uvp.family.services

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import rut.uvp.family.models.ActivityRequestData
import rut.uvp.family.models.SelectedTimeSlot
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class DateSelectionService(
    private val chatClient: ChatClient,
    private val vectorStore: VectorStore,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val DATE_SELECTION_PROMPT = """
            Ты - планировщик семейного досуга. Тебе нужно выбрать оптимальное время для проведения семейного мероприятия, 
            основываясь на доступных временных слотах членов семьи и информации о запросе.
            
            Информация о запросе:
            {{request}}
            
            Доступные временные слоты:
            {{availableSlots}}
            
            Проанализируй доступные временные слоты и выбери оптимальное время для мероприятия.
            Учитывай возраст члена семьи при выборе (например, для детей младшего возраста не стоит выбирать поздние вечерние часы).
            
            Верни результат ТОЛЬКО в следующем JSON формате:
            {
              "selectedDate": "YYYY-MM-DD",
              "selectedTimeRange": "HH:MM-HH:MM"
            }
        """
    }
    
    /**
     * Gets available time slots for a family member from the RAG store
     *
     * @param familyRole The role of the family member (e.g., "daughter", "son")
     * @return A list of available time slots
     */
    fun getAvailableTimeSlots(familyRole: String?): List<String> {
        if (familyRole == null) return emptyList()
        
        // Create a query to find available slots for the specified family member
        val query = "доступное время для $familyRole"
        
        // Search the vector store for relevant documents
        val results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(5)
                .build()
        )
        
        // If no results found, create some dummy data for demonstration
        if (results.isNullOrEmpty()) {
            // In a real application, you would handle this differently
            return generateDummyTimeSlots()
        }
        
        // Extract available time slots from the results
        return results.mapNotNull { it.text }
    }
    
    /**
     * Generates dummy time slots for demonstration purposes
     * In a real application, this would be replaced with actual data
     */
    private fun generateDummyTimeSlots(): List<String> {
        return listOf(
            "2023-09-30 10:00-13:00",
            "2023-09-30 15:00-18:00", 
            "2023-10-01 11:00-14:00",
            "2023-10-01 16:00-19:00"
        )
    }
    
    /**
     * Adds available time slots for a family member to the RAG store
     *
     * @param familyRole The role of the family member
     * @param timeSlots List of time slots in "YYYY-MM-DD HH:MM-HH:MM" format
     */
    fun addAvailableTimeSlots(familyRole: String, timeSlots: List<String>) {
        val documentText = "Доступное время для $familyRole: ${timeSlots.joinToString(", ")}"
        val document = Document.builder()
            .text(documentText)
            .metadata(mapOf("type" to "timeSlots", "familyRole" to familyRole))
            .build()
        
        vectorStore.add(listOf(document))
    }
    
    /**
     * Selects an optimal time slot based on the activity request
     *
     * @param request The activity request data
     * @return The selected time slot or null if no suitable slot is found
     */
    fun selectTimeSlot(request: ActivityRequestData): SelectedTimeSlot? {
        val familyRole = request.familyMember?.role ?: return null
        
        // Get available time slots
        val availableSlots = getAvailableTimeSlots(familyRole)
        if (availableSlots.isEmpty()) return null
        
        // Use LLM to select the optimal time slot
        val promptTemplate = SystemPromptTemplate.builder()
            .template(DATE_SELECTION_PROMPT)
            .parameter("request", objectMapper.writeValueAsString(request))
            .parameter("availableSlots", availableSlots.joinToString("\n"))
            .build()
            .create()
        
        val response = chatClient
            .prompt()
            .system(promptTemplate)
            .call()
            .content()
        
        return try {
            objectMapper.readValue<SelectedTimeSlot>(response)
        } catch (e: Exception) {
            println("Error parsing time slot selection: $e")
            println("Response content: $response")
            null
        }
    }
} 