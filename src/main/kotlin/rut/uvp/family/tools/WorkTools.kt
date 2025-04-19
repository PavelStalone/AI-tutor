package rut.uvp.family.tools

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.document.Document
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import rut.uvp.family.service.ConversationFlowService
import rut.uvp.family.service.DateSelectionService
import rut.uvp.family.service.KudaGoService
import rut.uvp.family.service.SearchQueryService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

@Component
class WorkTools(
    private val vectorStore: VectorStore,
    @Qualifier("GenerationClient") private val chatClient: ChatClient,
    @Autowired private val conversationFlowService: ConversationFlowService,
    @Autowired private val dateSelectionService: DateSelectionService,
    @Autowired private val searchQueryService: SearchQueryService,
    @Autowired private val kudaGoService: KudaGoService
) {
    private val objectMapper = jacksonObjectMapper()

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

    @Tool(description = "Парсинг запроса пользователя для досуга. Дата должна быть в формате UNIX timestamp (целое число)")
    fun parseLeisureRequest(@ToolParam(description = "Текст запроса") message: String): String {
        val req = conversationFlowService.parseUserMessage(message)
        return objectMapper.writeValueAsString(req)
    }

    @Tool(description = "Подбор даты для семейного досуга")
    fun selectLeisureDate(@ToolParam(description = "Список участников") members: List<String>): String {
        val (date, timeRange) = dateSelectionService.selectDate(members)
        return objectMapper.writeValueAsString(mapOf("selected_date" to date, "selected_time_range" to timeRange))
    }

    @Tool(description = "Поиск мероприятий через KudaGo")
    fun searchLeisureEvents(@ToolParam(description = "Параметры поиска") params: Map<String, String>): String {
        val events = kudaGoService.searchEvents(params)
        return objectMapper.writeValueAsString(events)
    }
}