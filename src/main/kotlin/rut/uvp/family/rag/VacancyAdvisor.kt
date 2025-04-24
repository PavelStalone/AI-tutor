package rut.uvp.family.rag

import org.springframework.ai.chat.client.advisor.api.*
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

class VacancyAdvisor(
    private val vectorStore: VectorStore,
    private val searchRequest: SearchRequest = SearchRequest.builder().build(),
) : CallAroundAdvisor, StreamAroundAdvisor {

    override fun getOrder(): Int = DEFAULT_ORDER

    override fun getName(): String = this::class.java.simpleName

    override fun aroundStream(advisedRequest: AdvisedRequest, chain: StreamAroundAdvisorChain): Flux<AdvisedResponse> {
        val adviseResponses = Mono.just(advisedRequest)
            .publishOn(Schedulers.boundedElastic())
            .map { /* BEFORE */ }
            .flatMapMany { request ->
                chain.nextAroundStream(request)
            }

        // AFTER
    }

    override fun aroundCall(advisedRequest: AdvisedRequest, chain: CallAroundAdvisorChain): AdvisedResponse {
        TODO("Not yet implemented")
    }

    private fun AdviseRequest.before(): AdviseRequest {

    }

    companion object {
        private const val DEFAULT_USER_TEXT_ADVISE =
            "\nContext information is below, surrounded by ---------------------\n\n---------------------\n{question_answer_context}\n---------------------\n\nGiven the context and provided history information and not prior knowledge,\nreply to the user comment. If the answer is not in the context, inform\nthe user that you can't answer the question.\n"
        private const val RETRIEVED_DOCUMENTS = "qa_retrieved_documents"
        private const val DEFAULT_ORDER = 1
    }
}