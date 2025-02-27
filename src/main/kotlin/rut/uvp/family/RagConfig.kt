package rut.uvp.family

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.reader.TextReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.io.File

@Configuration
class RagConfig {

    @Value("classpath:/TestRag.txt")
    private lateinit var testRag: Resource

    @Bean
    fun provideVectorStore(embeddingModel: EmbeddingModel): VectorStore {
        val vectorStore = SimpleVectorStore.builder(embeddingModel).build()

        val vectorStoreFile = File("./vector_store.json")

        if (vectorStoreFile.exists()) {
            println("VectorStore loaded")

            vectorStore.load(vectorStoreFile)
        } else {
            println("Create vectorFile")

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

            vectorStore.add(splitDocuments)
            vectorStore.save(vectorStoreFile)
        }

        println("VectorStore initialized success")
        return vectorStore
    }
}
