package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.FamilyLeisureRequest

@Service
class SearchQueryService {
    fun buildKudaGoQuery(request: FamilyLeisureRequest): Map<String, String> {
        val params = mutableMapOf<String, String>()
        params["city"] = request.city ?: "msk"
        request.date?.let { params["actual_since"] = it } // KudaGo поддерживает actual_since/actual_until (timestamp)
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