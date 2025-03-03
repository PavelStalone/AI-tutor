package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OllamaConfig.OllamaClientProperties::class)
class OllamaConfig {

    @ConfigurationProperties(prefix = "spring.ai.ollama")
    data class OllamaClientProperties(
        val baseUrl: String,
        val model: String
    )

    @Bean
    fun chatModel(properties: OllamaClientProperties): ChatModel {
        return OllamaChatModel.builder()
            .ollamaApi(OllamaApi(properties.baseUrl))
            .defaultOptions(
                OllamaOptions.builder()
                    .model(properties.model)
                    .temperature(0.4)
                    .build()
            )
            .build()
    }

    @Bean
    fun chatClient(
        chatModel: ChatModel,
        vectorStore: VectorStore,
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(
                QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().build())
            )
            .build()
    }
}

class TestTools {

    @Tool(description = "Записаться на собеседование по названию компании")
    fun testFun(companyName: String): String {
        println("testFun called: $companyName")
        return "Собеседование назначено на 28.02.2025"
    }
}
