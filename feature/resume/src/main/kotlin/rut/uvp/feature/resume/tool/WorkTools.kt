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
import rut.uvp.deepsearch.service.DeepSearchService
import rut.uvp.feature.resume.service.VacancyStoreService
import kotlin.time.Duration.Companion.days

@Component
class WorkTools(
    @Qualifier(ChatClientQualifier.QUERY_GENERATOR_CLIENT)
    private val chatClient: ChatClient,
    private val deepSearchService: DeepSearchService,
    private val vacancyStoreService: VacancyStoreService,
) {

    @Tool(description = "Функция для получения новых вакансий")
    fun fetchCompany(@ToolParam(description = "Стек пользователя") stack: String): String = runCatching {
        println("fetchCompany called: $stack")

        val query =
            chatClient
                .prompt("Сделай ссылку для поиска вакансий для пользователя со стеком: $stack")
                .call()
                .content()
        requireNotNull(query)

        val result = runBlocking {
            deepSearchService.deepSearch(query)
        }

        Log.v("Result: $result")

        result.forEach { info ->
            vacancyStoreService.addWithTTL(text = info.toString(), instant = Clock.System.now().plus(2.days))
        }

        return vacancyStoreService
            .search(
                SearchRequest.builder()
                    .query(stack)
                    .topK(20)
                    .similarityThreshold(0.4)
                    .build()
            )
            .map(Document::getText)
            .joinToString(System.lineSeparator())
            .also { println("Return result: $it") }
    }.getOrDefault("")
}
