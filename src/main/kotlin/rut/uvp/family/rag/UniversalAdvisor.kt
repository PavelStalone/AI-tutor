package rut.uvp.family.rag

import org.springframework.ai.chat.client.advisor.api.*
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class UniversalAdvisor private constructor(
    private val order: Int = 0,
    private val advisorName: String,
    private val advisorPrompt: String,
    private val searchRequest: SearchRequest,
    private val contextAnswerProperty: String,
    private val retrievedDocumentsProperty: String,
    private val documentStore: (searchRequest: SearchRequest) -> List<Document>,
) : CallAroundAdvisor, StreamAroundAdvisor {

    override fun getOrder(): Int = order

    override fun getName(): String = advisorName

    override fun aroundStream(advisedRequest: AdvisedRequest, chain: StreamAroundAdvisorChain): Flux<AdvisedResponse> {
        val adviseResponses = Mono.just(advisedRequest)
            .publishOn(Schedulers.boundedElastic())
            .map { request -> request.before() }
            .flatMapMany { request ->
                chain.nextAroundStream(request)
            }

        return adviseResponses.map { response ->
            if (onFinishReason(response)) {
                response.after()
            } else {
                response
            }
        }
    }

    override fun aroundCall(advisedRequest: AdvisedRequest, chain: CallAroundAdvisorChain): AdvisedResponse {
        TODO("Not yet implemented")
    }

    private fun AdvisedRequest.before(): AdvisedRequest {
        val context = HashMap(adviseContext())
        val userText = userText()
        val userParams = userParams()

        val advisedUserText = userText + System.lineSeparator() + advisorPrompt
        val query = PromptTemplate(userText, userParams).render()

        val searchRequestToUse = SearchRequest
            .from(searchRequest)
            .query(query)
            .build()
        val documents = documentStore(searchRequestToUse)

        context[retrievedDocumentsProperty] = documents

        val documentContext = documents
            .map(Document::getText)
            .joinToString(System.lineSeparator())
        val advisedUserParams = HashMap(userParams)

        advisedUserParams[contextAnswerProperty] = documentContext

        val advisedRequest = AdvisedRequest
            .from(this)
            .userText(advisedUserText)
            .userParams(advisedUserParams)
            .adviseContext(context)
            .build()

        return advisedRequest
    }

    private fun AdvisedResponse.after(): AdvisedResponse {
        val chatResponseBuilder = ChatResponse.builder().from(response())
        chatResponseBuilder.metadata(retrievedDocumentsProperty, adviseContext()[retrievedDocumentsProperty])
        return AdvisedResponse(chatResponseBuilder.build(), adviseContext())
    }

    private fun onFinishReason(response: AdvisedResponse): Boolean {
        return response.response?.results?.any { result ->
            result != null && result.metadata != null && StringUtils.hasText(result.metadata.finishReason)
        } ?: false
    }


    companion object Factory {
        private val DEFAULT_ADVISOR_PROMPT: (answer: String) -> String = { answer ->
            """
            Контекстная информация приведена ниже, обрамлена ---------------------
            
            ---------------------
            $answer
            ---------------------
            
            Учитывая контекст и предоставленную историю информации, а не предварительные знания,
            ответьте на комментарий пользователя. Если ответа нет в контексте, сообщите
            пользователю, что вы не можете ответить на вопрос.
            """.trimIndent()
        }

        private const val DEFAULT_CONTEXT_ANSWER_PROPERTY = "answer_context"
        private const val DEFAULT_RETRIEVED_DOCUMENTS_PROPERTY = "retrieved_documents"

        fun create(
            documentStore: (searchRequest: SearchRequest) -> List<Document>,
            order: Int = 0,
            advisorName: String = UniversalAdvisor::class.java.simpleName,
            searchRequest: SearchRequest = SearchRequest.builder().build(),
            contextAnswerProperty: String = DEFAULT_CONTEXT_ANSWER_PROPERTY,
            advisorPrompt: (answer: String) -> String = DEFAULT_ADVISOR_PROMPT,
            retrievedDocumentsProperty: String = DEFAULT_RETRIEVED_DOCUMENTS_PROPERTY,
        ): UniversalAdvisor = UniversalAdvisor(
            order = order,
            advisorName = advisorName,
            documentStore = documentStore,
            searchRequest = searchRequest,
            contextAnswerProperty = contextAnswerProperty,
            retrievedDocumentsProperty = retrievedDocumentsProperty,
            advisorPrompt = advisorPrompt("{$contextAnswerProperty}"),
        )

        fun create(
            vectorStore: VectorStore,
            order: Int = 0,
            advisorName: String = UniversalAdvisor::class.java.simpleName,
            searchRequest: SearchRequest = SearchRequest.builder().build(),
            contextAnswerProperty: String = DEFAULT_CONTEXT_ANSWER_PROPERTY,
            advisorPrompt: (answer: String) -> String = DEFAULT_ADVISOR_PROMPT,
            retrievedDocumentsProperty: String = DEFAULT_RETRIEVED_DOCUMENTS_PROPERTY,
        ): UniversalAdvisor = create(
            order = order,
            advisorName = advisorName,
            searchRequest = searchRequest,
            advisorPrompt = advisorPrompt,
            contextAnswerProperty = contextAnswerProperty,
            retrievedDocumentsProperty = retrievedDocumentsProperty,
            documentStore = { request -> vectorStore.similaritySearch(request)!! },
        )
    }
}
