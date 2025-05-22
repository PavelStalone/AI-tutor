package rut.uvp.app.controller

import org.springframework.ai.chat.client.ChatClient
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
import rut.uvp.core.ai.rag.UniversalAdvisor
import rut.uvp.core.common.log.Log
import rut.uvp.feature.resume.service.VacancyStoreService
import rut.uvp.feature.resume.store.UserStoreImpl

@RestController
@RequestMapping("chat")
class ChatController(
    private val chatClient: ChatClient,
    private val embeddingModel: EmbeddingModel,
    private val vacancyStoreService: VacancyStoreService,
) {

    @PostMapping(produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun sendMessage(@RequestBody messageRequest: MessageRequest): Flux<String> {
        Log.v("Incoming message: $messageRequest")

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
            .apply {
                resumeStore.getResume("test_resume")?.let { vectorStore ->
                    advisors(
                        UniversalAdvisor.create(
                            order = 0,
                            vectorStore = vectorStore,
                            searchRequest = SearchRequest.builder().topK(10).build(),
                            contextAnswerProperty = "stek_answer_context",
                            retrievedDocumentsProperty = "stek_retrieved_documents",
                            advisorPrompt = { answer ->
                                """
                                    ----- ДАННЫЕ ИЗ РЕЗЮМЕ -----
                                    $answer
                                    ---------------------------
                                """.trimIndent()
                            }
                        )
                    )
                }
            }
            .advisors(
                UniversalAdvisor.create(
                    order = 1,
                    advisorName = "VacancyAdvisor",
                    contextAnswerProperty = "vacancy_answer_context",
                    searchRequest = SearchRequest.builder().topK(10).similarityThreshold(0.4).build(),
                    retrievedDocumentsProperty = "vacancy_retrieved_documents",
                    documentStore = { searchRequest -> vacancyStoreService.search(searchRequest) },
                    advisorPrompt = { answer ->
                        """
                            ----- ДАННЫЕ О ВАКАНСИЯХ -----
                            $answer
                            ------------------------------
                        """.trimIndent()
                    }
                ),
            )
            .stream()
            .content()
    }

    data class MessageRequest(
        val message: String,
        val resumeId: String,
    )
}
