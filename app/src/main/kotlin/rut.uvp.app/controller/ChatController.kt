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
import rut.uvp.feature.resume.tool.WorkTools

@RestController
@RequestMapping("chat")
class ChatController(
    private val workTools: WorkTools,
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

        resumeStore.saveResume("test_resume", "{\n" +
                "  \"id\": 1747942707717,\n" +
                "  \"content\": \"{\\\"ROOT\\\":{\\\"type\\\":\\\"div\\\",\\\"isCanvas\\\":true,\\\"props\\\":{\\\"className\\\":\\\"bg-white w-full min-h-[297mm] rounded-[26px] p-5 shadow-md\\\",\\\"style\\\":{\\\"margin\\\":\\\"0\\\"}},\\\"displayName\\\":\\\"div\\\",\\\"custom\\\":{},\\\"hidden\\\":false,\\\"nodes\\\":[\\\"_apdL6gcIw\\\",\\\"j_tZcuSsco\\\",\\\"EeOpcqtHwA\\\",\\\"CmHJvk0YWj\\\",\\\"voeakHMInG\\\"],\\\"linkedNodes\\\":{}},\\\"_apdL6gcIw\\\":{\\\"type\\\":{\\\"resolvedName\\\":\\\"ModernHeader\\\"},\\\"isCanvas\\\":true,\\\"props\\\":{\\\"name\\\":\\\"John Doe\\\",\\\"title\\\":\\\"Software Developer\\\",\\\"contact\\\":\\\"john@example.com | (123) 456-7890\\\"},\\\"displayName\\\":\\\"Header\\\",\\\"custom\\\":{},\\\"parent\\\":\\\"ROOT\\\",\\\"hidden\\\":false,\\\"nodes\\\":[],\\\"linkedNodes\\\":{}},\\\"j_tZcuSsco\\\":{\\\"type\\\":{\\\"resolvedName\\\":\\\"ModernSummary\\\"},\\\"isCanvas\\\":true,\\\"props\\\":{\\\"text\\\":\\\"Опытный разработчик с фокусом на создании современных веб-приложений. Специализируюсь на React и TypeScript. Имею сильные навыки в оптимизации производительности и создании отзывчивых интерфейсов.\\\"},\\\"displayName\\\":\\\"Summary\\\",\\\"custom\\\":{},\\\"parent\\\":\\\"ROOT\\\",\\\"hidden\\\":false,\\\"nodes\\\":[],\\\"linkedNodes\\\":{}},\\\"EeOpcqtHwA\\\":{\\\"type\\\":{\\\"resolvedName\\\":\\\"ModernExperience\\\"},\\\"isCanvas\\\":true,\\\"props\\\":{\\\"items\\\":[{\\\"id\\\":\\\"experience-1\\\",\\\"title\\\":\\\"Frontend Developer\\\",\\\"company\\\":\\\"Tech Company\\\",\\\"location\\\":\\\"Москва, Россия\\\",\\\"period\\\":\\\"2021 - настоящее время\\\",\\\"responsibilities\\\":[\\\"Разработка пользовательских интерфейсов на React\\\",\\\"Оптимизация производительности приложений\\\",\\\"Работа с REST API и GraphQL\\\"]}]},\\\"displayName\\\":\\\"Experience\\\",\\\"custom\\\":{},\\\"parent\\\":\\\"ROOT\\\",\\\"hidden\\\":false,\\\"nodes\\\":[],\\\"linkedNodes\\\":{}},\\\"CmHJvk0YWj\\\":{\\\"type\\\":{\\\"resolvedName\\\":\\\"ModernEducation\\\"},\\\"isCanvas\\\":true,\\\"props\\\":{\\\"items\\\":[{\\\"degree\\\":\\\"Бакалавр Компьютерных Наук\\\",\\\"institution\\\":\\\"Московский Технический Университет\\\",\\\"location\\\":\\\"Москва, Россия\\\",\\\"period\\\":\\\"2017 - 2021\\\",\\\"description\\\":\\\"Специализация в разработке программного обеспечения и искусственном интеллекте\\\"}]},\\\"displayName\\\":\\\"Education\\\",\\\"custom\\\":{},\\\"parent\\\":\\\"ROOT\\\",\\\"hidden\\\":false,\\\"nodes\\\":[],\\\"linkedNodes\\\":{}},\\\"voeakHMInG\\\":{\\\"type\\\":{\\\"resolvedName\\\":\\\"ModernSkills\\\"},\\\"isCanvas\\\":true,\\\"props\\\":{\\\"categories\\\":[{\\\"id\\\":\\\"category-1\\\",\\\"name\\\":\\\"Технические навыки\\\",\\\"skills\\\":[\\\"JavaScript\\\",\\\"React\\\",\\\"Node.js\\\",\\\"TypeScript\\\",\\\"GraphQL\\\",\\\"Docker\\\"]},{\\\"id\\\":\\\"category-2\\\",\\\"name\\\":\\\"Soft Skills\\\",\\\"skills\\\":[\\\"Командная работа\\\",\\\"Коммуникация\\\",\\\"Управление проектами\\\",\\\"Agile/Scrum\\\"]}]},\\\"displayName\\\":\\\"Skills\\\",\\\"custom\\\":{},\\\"parent\\\":\\\"ROOT\\\",\\\"hidden\\\":false,\\\"nodes\\\":[],\\\"linkedNodes\\\":{}}}\",\n" +
                "  \"createdAt\": \"2025-05-22T19:38:27.717Z\",\n" +
                "  \"lastModified\": \"2025-05-22T19:38:53.880Z\",\n" +
                "  \"isDraft\": false,\n" +
                "  \"templateId\": \"modern\",\n" +
                "  \"name\": \"JSON\"\n" +
                "}")

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
            .tools(workTools)
            .stream()
            .content()
    }

    data class MessageRequest(
        val message: String,
        val resumeId: String,
    )
}
