package rut.uvp.family.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Модель для города KudaGo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoLocation(
    val id: Int,
    val name: String,
    val slug: String,
    @JsonProperty("timezone") val timeZone: String,
    val currency: String? = null
)

/**
 * Ответ с списком городов
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoLocationResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<KudaGoLocation>
)

/**
 * Модель для категории KudaGo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoCategory(
    val id: Int,
    val slug: String,
    val name: String
)

/**
 * Ответ с списком категорий
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoCategoryResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<KudaGoCategory>
)

/**
 * Модель изображения в KudaGo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoImage(
    val image: String,
    val source: KudaGoImageSource? = null
)

/**
 * Источник изображения
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoImageSource(
    val name: String?,
    val link: String?
)

/**
 * Место проведения
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoPlace(
    val id: Int?,
    val title: String?,
    val address: String?,
    val slug: String?
)

/**
 * Даты проведения
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoDateRange(
    val start: Long, // Unix timestamp
    val end: Long?, // Unix timestamp
    @JsonProperty("start_date") val startDate: String?, // ISO
    @JsonProperty("end_date") val endDate: String?, // ISO
    @JsonProperty("start_time") val startTime: String?, // HH:MM:SS
    @JsonProperty("end_time") val endTime: String? // HH:MM:SS
)

/**
 * Стоимость
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoPrice(
    val min: Int?,
    val max: Int?
)

/**
 * Модель для мероприятия KudaGo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoEvent(
    val id: Int,
    val publication_date: Long, // Unix timestamp
    val dates: List<KudaGoDateRange>,
    val title: String,
    val slug: String,
    val description: String?,
    @JsonProperty("short_description") val shortDescription: String?,
    val place: KudaGoPlace?,
    val price: String?,
    val images: List<KudaGoImage> = emptyList(),
    val categories: List<KudaGoCategory> = emptyList(),
    @JsonProperty("age_restriction") val ageRestriction: String?,
    val location: KudaGoLocation? = null
)

/**
 * Ответ с списком мероприятий
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KudaGoEventResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<KudaGoEvent>
)

/**
 * Параметры поиска для API KudaGo
 */
data class KudaGoSearchParams(
    val location: String? = null, // slug города (msk, spb, etc)
    val categories: List<String> = emptyList(), // slugs категорий (concert, exhibition, etc)
    val actualSince: Instant? = null, // Начало периода актуальности (Unix timestamp)
    val actualUntil: Instant? = null, // Конец периода актуальности (Unix timestamp)
    val ageRestriction: String? = null, // Возрастное ограничение
    val isFree: Boolean? = null, // Бесплатное мероприятие
    val query: String? = null, // Поисковый запрос
    val pageSize: Int = 10, // Размер страницы
    val page: Int = 1 // Номер страницы
) 