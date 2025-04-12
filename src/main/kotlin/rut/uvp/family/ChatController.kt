package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("chat")
class ChatController(
    private val testTools: WorkTools,
    private val familyActivityTools: FamilyActivityTools,
    private val chatClient: ChatClient,
    private val embeddingModel: EmbeddingModel,
) {

    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun sendMessage(@RequestBody messageRequest: MessageRequest): Flux<String> {
        println("message: $messageRequest")

        val resumeStore = UserStoreImpl(
            userId = "user",
            textSplitter = TokenTextSplitter(
                500,
                200,
                10,
                5000,
                false
            ),
            vectorStoreBuilder = SimpleVectorStore.builder(embeddingModel)
        )

        resumeStore.saveResume("test_resume", "Мой стек технологий: Java, Python на среднем уровне, Git, HTML и CSS")

        return chatClient
            .prompt(messageRequest.message)
//            .apply {
//                resumeStore.getResume("test_resume")?.let { vectorStore ->
//                    println("Added advisor")
//                    advisors(QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().topK(10).build()))
//                }
//            }
            .tools(testTools, familyActivityTools)
            .stream()
            .content()
    }

    data class MessageRequest(
        val message: String
    )
}
