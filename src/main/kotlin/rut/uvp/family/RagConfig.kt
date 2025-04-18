package rut.uvp.family

import org.springframework.ai.chat.client.advisor.api.*
import org.springframework.ai.chat.model.MessageAggregator
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource
import reactor.core.publisher.Flux

@Configuration
class RagConfig {

    @Value("classpath:/TestRag.txt")
    private lateinit var testRag: Resource

    @Bean
    @Order(1)
    fun clearVectorStore(vectorStore: VectorStore) = CommandLineRunner { _ ->
        println("Start clear")
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("All")
                .similarityThreshold(0.0)
                .topK(Int.MAX_VALUE)
                .build()
        )
            ?.map {
                println("Clear: $it")
                it.id
            }
            ?.run {
                if (isNotEmpty()) {
                    vectorStore.delete(this)
                }
            }
        println("Finished")
    }

//    @Bean
//    fun provideVectorStore(embeddingModel: EmbeddingModel): VectorStore {
//        val vectorStore = SimpleVectorStore.builder(embeddingModel).build()
//
//        val vectorStoreFile = File("./vector_store.json")
//
//        if (vectorStoreFile.exists()) {
//            println("VectorStore loaded")
//
//            vectorStore.load(vectorStoreFile)
//        } else {
//            println("Create vectorFile")
//
//            val textReader = TextReader(testRag)
//            textReader.customMetadata["filename"] = "TestRag.txt"
//
//            val documents = textReader.get()
//
//            val textSplitter = TokenTextSplitter(
//                500,
//                200,
//                10,
//                5000,
//                false
//            )
//
//            val splitDocuments = textSplitter.apply(documents)
//
//            vectorStore.add(splitDocuments)
//            vectorStore.save(vectorStoreFile)
//        }
//
//        println("VectorStore initialized success")
//        return vectorStore
//    }

//    @Bean
//    @Order(1)
//    fun runnerRag(vectorStore: VectorStore) = CommandLineRunner { _ ->
//        val textReader = TextReader(testRag)
//        textReader.customMetadata["filename"] = "TestRag.txt"
//
//        val documents = textReader.get()
//
//        val textSplitter = TokenTextSplitter(
//            500,
//            200,
//            10,
//            5000,
//            false
//        )
//
//        val splitDocuments = textSplitter.apply(documents)
//
//        splitDocuments.filter { document ->
//            println("Document. id: ${document.id}, text: ${document.text}")
//            document.text ?: false
//
//            val similar = vectorStore.similaritySearch(document.text!!)
//            println("similar: $similar")
//
//            (similar?.any { sim -> sim.text == document.text } != true)
//        }.also {
//            println("Document was added: $it")
//        }.run(vectorStore::add)
//
//        println("Finished")
//    }
}

class LoggerAdvisor() : CallAroundAdvisor, StreamAroundAdvisor {

    override fun getOrder(): Int {
        return 0
    }

    override fun getName(): String {
        return this::class.java.simpleName
    }

    override fun aroundStream(advisedRequest: AdvisedRequest, chain: StreamAroundAdvisorChain): Flux<AdvisedResponse> {
        println("BEFORE Stream: $advisedRequest")

        val advisedResponse = chain.nextAroundStream(advisedRequest)

        return MessageAggregator().aggregateAdvisedResponse(advisedResponse) { println("AFTER Stream: $it") }
    }

    override fun aroundCall(advisedRequest: AdvisedRequest, chain: CallAroundAdvisorChain): AdvisedResponse {
        println("BEFORE Call: $advisedRequest")

        val advisedResponse = chain.nextAroundCall(advisedRequest)

        println("AFTER Call: $advisedResponse")

        return advisedResponse
    }
}
