package rut.uvp.family

import org.springframework.ai.reader.TextReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.Resource

@Configuration
class RagConfig {

    @Value("classpath:/TestRag.txt")
    private lateinit var testRag: Resource

    @Bean
    @Order(1)
    fun runnerRag(vectorStore: VectorStore) = CommandLineRunner { _ ->
        val textReader = TextReader(testRag)
        textReader.customMetadata["filename"] = "TestRag.txt"

        val documents = textReader.get()

        val textSplitter = TokenTextSplitter(
            500,
            200,
            10,
            5000,
            false
        )

        val splitDocuments = textSplitter.apply(documents)

        splitDocuments.filter { document ->
            println("Document. id: ${document.id}, text: ${document.text}")
            document.text ?: false

            val similar = vectorStore.similaritySearch(document.text!!)
            println("similar: $similar")

            (similar?.any { sim -> sim.text == document.text } != true)
        }.also {
            println("Document was added: $it")
        }.run(vectorStore::add)

        println("Finished")
    }
}
