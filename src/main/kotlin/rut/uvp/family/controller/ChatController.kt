package rut.uvp.family.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import rut.uvp.family.service.ConversationFlowService
import rut.uvp.family.service.DateSelectionService
import rut.uvp.family.service.KudaGoService
import rut.uvp.family.service.SearchQueryService
import java.nio.file.Files

@RestController
@RequestMapping("chat")
class ChatController(
    private val conversationFlowService: ConversationFlowService,
    private val dateSelectionService: DateSelectionService,
    private val searchQueryService: SearchQueryService,
    private val kudaGoService: KudaGoService
) {
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun sendMessage(@RequestBody messageRequest: MessageRequest): ResponseEntity<Any> {
        val leisureRequest = conversationFlowService.parseUserMessage(messageRequest.message)
        val finalRequest = if (leisureRequest.date == null || leisureRequest.date == "auto") {
            val (date, timeRange) = dateSelectionService.selectDate(leisureRequest.members?.map { it.role } ?: emptyList())
            leisureRequest.copy(date = date)
        } else leisureRequest
        val query = searchQueryService.buildKudaGoQuery(finalRequest)
        query.plus("location" to query["city"])
        query.filter { it.key != "city" }
        val events = kudaGoService.searchEvents(query)
        return ResponseEntity.ok(events)
    }

    @GetMapping
    fun chatPage(): ResponseEntity<Any> {
        val resource = ClassPathResource("frontend/index.html")
        if (!resource.exists()) return ResponseEntity.notFound().build()
        val content = resource.inputStream.readBytes()
        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_HTML
        return ResponseEntity(content, headers, HttpStatus.OK)
    }

    data class MessageRequest(val message: String)
}
