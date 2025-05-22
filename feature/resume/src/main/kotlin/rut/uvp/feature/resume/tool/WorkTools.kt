package rut.uvp.feature.resume.tool

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.Document
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import rut.uvp.core.ai.config.ChatClientQualifier
import rut.uvp.core.common.log.Log
import rut.uvp.feature.deepsearch.domain.model.Vacancy
import rut.uvp.feature.deepsearch.service.DeepSearchService
import rut.uvp.feature.resume.service.VacancyStoreService
import kotlin.time.Duration.Companion.days

@Component
class WorkTools(
    @Qualifier(ChatClientQualifier.QUERY_GENERATOR_CLIENT)
    private val chatClient: ChatClient,
    private val deepSearchService: DeepSearchService,
    private val vacancyStoreService: VacancyStoreService,
) {

    @Tool(description = "Получение новых вакансий")
    fun fetchCompany(
        @ToolParam(description = "Комплексный профиль навыков и предпочтений пользователя, включающий ключевые технологии, инструменты, уровень опыта, тип занятости, предпочтения по локации, зарплате и другие важные параметры")
        profile: String
    ): String = runCatching {
        println("fetchCompany called: $profile")

        val query =
            chatClient
                .prompt(profile)
                .call()
                .content()
        requireNotNull(query)

        val result = runBlocking {
            deepSearchService.deepSearch(query)
        }

        Log.v("Result: $result")

        val vacancyFromStore = runCatching {
            result.forEach { info ->
                vacancyStoreService.addWithTTL(text = info.toString(), instant = Clock.System.now().plus(2.days))
            }

            vacancyStoreService
                .search(
                    SearchRequest.builder()
                        .query(profile)
                        .topK(20)
                        .similarityThreshold(0.4)
                        .build()
                )
                .map(Document::getText)
        }.onFailure { throwable ->
            Log.e(throwable, "Failed update vectorStore")
        }.getOrNull()

        return (vacancyFromStore ?: result.map(Vacancy::toString))
            .joinToString(System.lineSeparator())
            .also { println("Return result: $it") }
            .let {info ->
                """
                    Данные об вакансиях: $info
                    Используй только их для ответа. В ответе к каждой вакансии прикрепляй ссылку (url).
                    Предложи минимум 5 вакансий
                """.trimIndent()
            }
    }.getOrDefault("")
}
