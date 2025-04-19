package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.FamilyLeisureRequest
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.ZoneOffset

@Service
class SearchQueryService {
    fun buildKudaGoQuery(request: FamilyLeisureRequest): Map<String, String> {
        val params = mutableMapOf<String, String>()
        params["city"] = request.city ?: "msk"
        request.date?.let { dateStr ->
            val actualSince = try {
                when {
                    dateStr.matches(Regex("^\\d{10}$")) -> dateStr // уже timestamp в секундах
                    dateStr.matches(Regex("^\\d{13}$")) -> (dateStr.toLong() / 1000).toString() // миллисекунды
                    else -> LocalDate.parse(dateStr).atStartOfDay().toEpochSecond(ZoneOffset.UTC).toString() // ISO-строка
                }
            } catch (e: DateTimeParseException) {
                dateStr // fallback: что пришло
            }
            params["actual_since"] = actualSince
        }
        // Категории: сопоставление предпочтений с категориями KudaGo
        val categoryMap = mapOf(
            "театр" to "theatre",
            "детские" to "kids",
            "музей" to "museum",
            "кино" to "cinema",
            "концерт" to "concert",
            "парк" to "park"
        )
        // Собираем интересы всех членов семьи
        val allInterests = (request.members?.flatMap { it.interests ?: emptyList() } ?: emptyList()) + (request.preferences ?: emptyList())
        val categories = allInterests.mapNotNull { categoryMap[it.lowercase()] }.distinct().joinToString(",")
        if (categories.isNotBlank()) params["categories"] = categories
        // Можно добавить фильтрацию по возрасту, если нужно
        return params
    }
}