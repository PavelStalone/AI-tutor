package rut.uvp.family

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.ai.chat.client.ChatClient
import reactor.core.publisher.Flux

/**
 * Контроллер для обработки чат-запросов пользователей
 */
@RestController
@RequestMapping("chat")
class ChatController(
    private val familyActivityTools: FamilyActivityTools,
    private val chatClient: ChatClient
) {
    /**
     * Обрабатывает сообщения пользователя и отправляет ответы
     */
    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sendMessage(@RequestBody messageRequest: MessageRequest): Flux<String> {
        println("Received message: ${messageRequest.message}")

        return chatClient
            .prompt(messageRequest.message)
            .tools(familyActivityTools)
            .stream()
            .content()
    }

    /**
     * Модель запроса с сообщением пользователя
     */
    data class MessageRequest(
        val message: String
    )
}
