package rut.uvp.family.config

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class VectorStoreConfig {

    @Bean
    @Primary
    fun vectorStore(embeddingModel: EmbeddingModel): VectorStore {
        return SimpleVectorStore.builder(embeddingModel).build()
    }
} 