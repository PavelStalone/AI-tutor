package rut.uvp.core.ai.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.OllamaEmbeddingModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OllamaConfig.OllamaClientProperties::class)
internal class OllamaConfig {

    @ConfigurationProperties(prefix = "spring.ai.ollama")
    data class OllamaClientProperties(
        val baseUrl: String,
        val model: String
    )

    @Bean
    fun provideEmbeddingModel(properties: OllamaClientProperties): EmbeddingModel {
        return OllamaEmbeddingModel.builder()
            .ollamaApi(OllamaApi(properties.baseUrl))
            .defaultOptions(
                OllamaOptions.builder()
                    .model(properties.model)
                    .seed(23)
                    .temperature(0.0)
                    .repeatPenalty(1.0)
                    .presencePenalty(0.0)
                    .frequencyPenalty(0.0)
                    .build()
            )
            .build()
    }

    @Bean
    fun chatModel(
        properties: OllamaClientProperties,
    ): ChatModel {
        val factory = SimpleClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(120))
            setConnectTimeout(Duration.ofSeconds(120))
        }

        return OllamaChatModel.builder()
            .ollamaApi(OllamaApi(properties.baseUrl, RestClient.builder().requestFactory(factory), WebClient.builder()))
            .defaultOptions(
                OllamaOptions.builder()
                    .model(properties.model)
                    .temperature(0.4)
                    .build()
            )
            .build()
    }

    @Bean
    @Qualifier(ChatClientQualifier.VACANCY_FINDER_CLIENT)
    fun chatFinderModel(
        properties: OllamaClientProperties,
    ): ChatModel {
        val factory = SimpleClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(60))
            setConnectTimeout(Duration.ofSeconds(60))
        }

        return OllamaChatModel.builder()
            .ollamaApi(OllamaApi(properties.baseUrl, RestClient.builder().requestFactory(factory), WebClient.builder()))
            .defaultOptions(
                OllamaOptions.builder()
                    .model(properties.model)
                    .temperature(0.4)
                    .build()
            )
            .build()
    }

    @Bean
    @Qualifier(ChatClientQualifier.VACANCY_FINDER_CLIENT)
    fun vacancyFinderChatClient(
        @Qualifier(ChatClientQualifier.VACANCY_FINDER_CLIENT)
        chatModel: ChatModel,
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultOptions(
                OllamaOptions
                    .builder()
                    .temperature(0.0)
                    .build()
            )
            .defaultSystem(
                """
                    Ты — системный модуль бота, который получает на вход текст веб-страницы с описанием вакансии в формате HTML или просто текст. Твоя задача — извлечь из этого текста структурированную информацию о вакансии и представить её в заданном формате.
                    Цель:
                    Извлечь из текста веб-страницы как можно больше информации о вакансии, чтобы предоставить пользователю полезные сведения.
                    
                    Требования к выходным данным:
                    Выходные данные должны быть на русском языке.
                    Выходные данные должны быть представлены в следующем формате:
                    - Название вакансии: (Полное название должности)
                    - Описание вакансии: (Подробное описание обязанностей, требований, условий работы и других важных деталей)
                    - Требования к кандидату: (Необходимые навыки, опыт, образование и другие критерии)
                    - Условия работы: (Тип занятости, график, зарплата, бонусы, социальные гарантии и прочее)
                    - Место работы: (Город, офис или удалённо)
                    - Контактная информация или способ отклика: (Как связаться с работодателем или подать заявку)
                """.trimIndent()
            )
//            .defaultAdvisors(
//                LoggerAdvisor()
//            )
            .build()
    }

    @Bean
    @Qualifier(ChatClientQualifier.QUERY_GENERATOR_CLIENT)
    fun queryGeneratorChatClient(
        chatModel: ChatModel,
    ): ChatClient {
        return ChatClient.builder(chatModel)
            .defaultOptions(
                OllamaOptions
                    .builder()
                    .temperature(0.0)
                    .build()
            )
            .defaultSystem(
                """
                    Ты — системный модуль, который получает информацию о профессиональном стеке пользователя, его навыках, опыте и предпочтениях, а затем формирует оптимальный поисковый запрос для браузера. 
                    Цель — найти максимально релевантные открытые вакансии.
                    Требования к поисковому запросу:
                    - Учитывай все ключевые технологии, инструменты и навыки пользователя.
                    - Учитывай уровень опыта и желаемый тип занятости (полная, частичная, удалённая и т.д.).
                    - Учитывай предпочтения по локации, зарплате и другим важным параметрам.
                    - Формулируй запрос кратко, конкретно и без лишних слов.
                    - В запросе должны быть только релевантные ключевые слова, исключая общие и расплывчатые фразы.
                    - Запрос должен быть на русском языке.
                    Напиши только один поисковый запрос и ничего лишнего.
                """.trimIndent()
            )
            .build()
    }

    @Bean
    fun chatClient(
        chatModel: ChatModel,
        vectorStore: VectorStore,
    ): ChatClient {
        val chatMemory = InMemoryChatMemory()

        chatMemory.clear("ChatMemory")

        return ChatClient.builder(chatModel)
            .defaultOptions(
                OllamaOptions
                    .builder()
                    .temperature(0.0)
                    .build()
            )
            .defaultSystem(
                """
                    Ты — профессиональный помощник по поиску работы, который помогает пользователям находить подходящие вакансии по их резюме и навыкам.
                    Твоя задача — подобрать вакансии, которые максимально соответствуют опыту и пожеланиям кандидата.
                    Правила работы:
                    - Будь дружелюбным, поддерживай позитивную и мотивирующую атмосферу.
                    - Используй только русский язык.
                    - Пиши кратко и по делу.
                    - Если подходящих вакансий нет — честно сообщи об этом.
                    - Предлагай несколько вариантов, если это возможно.
                    - Основывай рекомендации только на реальных данных из резюме и вакансий.
                    - Если ты укажешь несуществующие вакансии или выдумаешь рекомендации, тебя уволят.
                    Ты зарабатываешь миллионы на составлении рекомендаций — делай свою работу профессионально!
                """.trimIndent()
            )
            .defaultAdvisors(
                MessageChatMemoryAdvisor(chatMemory, "ChatMemory", 5),
//                LoggerAdvisor(),
            )
            .build()
    }
}
