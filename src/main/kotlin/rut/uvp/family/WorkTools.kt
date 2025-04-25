package rut.uvp.family

import kotlinx.datetime.Clock
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.Document
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import rut.uvp.family.service.VacancyStoreService
import kotlin.time.Duration.Companion.days

@Component
class WorkTools(
    private val vacancyStoreService: VacancyStoreService,
    @Qualifier("GenerationClient") private val chatClient: ChatClient,
) {

    @Tool(description = "Функция для получения новых вакансий")
    fun fetchCompany(@ToolParam(description = "Стек пользователя") stack: String): String? {
        println("fetchCompany called: $stack")

        val result =
            chatClient
                .prompt("Сгенерируй список из 3 вакансий, в котором должно быть название компании, должность и стек технологий для этой вакансии. Можешь добавить из $stack стека, некоторые пункты (Добавляй на рандом, можешь вообще не добавлять из этого списка). Ответь на Русском языке. Напиши только список")
                .call()
                .content()

        result?.let { info ->
            vacancyStoreService.addWithTTL(text = info, instant = Clock.System.now().plus(2.days))
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
    }
}
