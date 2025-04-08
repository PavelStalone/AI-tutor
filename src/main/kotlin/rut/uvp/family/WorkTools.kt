package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.Document
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class WorkTools(
    private val vectorStore: VectorStore,
    @Qualifier("GenerationClient") private val chatClient: ChatClient,
) {

    @Tool(description = "Записать на собеседование по названию компании")
    fun makeInterview(companyName: String): String {
        println("makeInterview called: $companyName")

        return "Запись прошла успешно"
    }

    @Tool(description = "Получение новых вакансий")
    fun fetchCompany(@ToolParam(description = "Стек пользователя") stack: String): String? {
        println("fetchCompany called: $stack")

        val result =
            chatClient
                .prompt("Сгенерируй список из 3 вакансий, в котором должно быть название компании, должность и стек технологий для этой вакансии. Можешь добавить из $stack стека, некоторые пункты (Добавляй на рандом, можешь вообще не добавлять из этого списка). Ответь на Русском языке. Напиши только список")
                .call()
                .content()

        result?.let { info ->
            println("Add document: $info")
            vectorStore.add(
                listOf(
                    Document(info)
                )
            )
        }

        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(stack)
                .topK(20)
                .similarityThreshold(0.5)
                .build()
        )?.fold("") { acc, document ->
            acc + "\n" + document.text
        }.also { println("Return result: $it") }
    }
}