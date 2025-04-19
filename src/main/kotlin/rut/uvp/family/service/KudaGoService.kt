package rut.uvp.family.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import rut.uvp.family.model.FamilyLeisureResponse

@Service
class KudaGoService(
    private val webClient: WebClient = WebClient.create("https://kudago.com/public-api/v1.4")
) {
    private val logger = LoggerFactory.getLogger(KudaGoService::class.java)

    //    @Cacheable("kudago_events")
    fun searchEvents(params: Map<String, String>): List<FamilyLeisureResponse> {
        return try {
            var finalUrl = ""
            val defaultParams = mapOf(
                "fields" to "title,description,dates,images",
//                "expand" to "images,dates",
                "lang" to "ru",
                "page" to "1",
                "page_size" to "4"
            )
            val mergedParams = defaultParams + params.filterKeys { it !in defaultParams.keys }
            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder.path("/events/")
                    mergedParams.forEach { (k, v) -> uriBuilder.queryParam(k, v) }
                    val built = uriBuilder.build()
                    finalUrl = built.toString()
                    built
                }
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
                )
                .header("Accept", "application/json")
                .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                .retrieve()
                .onStatus({ it != HttpStatus.OK }) {
                    val status = it.statusCode()
                    Mono.error(RuntimeException("KudaGo error: ${status.value()}"))
                }
                .bodyToMono(KudaGoEventsResponse::class.java)
                .doOnSubscribe { logger.info("KudaGo request URL: $finalUrl") }
                .block()
            response?.results?.map {
                FamilyLeisureResponse.fromKudaGoEvent(it)
            } ?: emptyList()
        } catch (ex: Exception) {
            logger.error("KudaGo request falling back to unknown exception", ex)
            listOf()
        }
    }
}

// DTO для парсинга ответа KudaGo
data class KudaGoEventsResponse(
    val count: Int?,
    val next: String?,
    val previous: String?,
    val results: List<KudaGoEvent>
)

data class KudaGoEvent(
    val id: Long?,
    val title: String?,
    val dates: List<KudaGoDate>?,
    val place: KudaGoPlace?,
    val description: String?,
    val images: List<KudaGoImage>?
)

data class KudaGoDate(
    val start_date: String?,
    val start_time: String?,
    val start: Long?,
    val end_date: String?,
    val end_time: String?,
    val end: Long?,
    val is_continuous: Boolean?,
    val is_endless: Boolean?,
    val is_startless: Boolean?,
    val schedules: List<Any>?,
    val use_place_schedule: Boolean?
)

data class KudaGoPlace(
    val id: Long?,
    val title: String?,
    val slug: String?,
    val address: String?,
    val phone: String?,
    val subway: String?,
    val location: String?,
    val site_url: String?,
    val is_closed: Boolean?,
    val coords: KudaGoCoords?,
    val is_stub: Boolean?
)

data class KudaGoCoords(
    val lat: Double?,
    val lon: Double?
)

data class KudaGoImage(
    val image: String?,
    val thumbnails: Map<String, String>?,
    val source: KudaGoImageSource?
)

data class KudaGoImageSource(
    val name: String?,
    val link: String?
)
