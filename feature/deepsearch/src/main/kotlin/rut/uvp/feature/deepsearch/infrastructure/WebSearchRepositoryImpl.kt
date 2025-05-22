package rut.uvp.deepsearch.infrastructure

import org.jsoup.Jsoup
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import rut.uvp.core.common.log.Log
import rut.uvp.deepsearch.domain.repository.SearchRepository

@Component
internal class WebSearchRepositoryImpl(
    private val webClient: WebClient
) : SearchRepository {

    override suspend fun getLinks(query: String, size: Int): List<String> {
        Log.v("url: https://duckduckgo.com/html?q=$query")
        val page = webClient.get()
            .uri("https://duckduckgo.com/html?q={query}", query)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .awaitBody<String>()

        val document = Jsoup.parse(page)
        return document.select("a.result__a")
            .asSequence()
            .map { element -> "https:" + element.attr("href") }
            .take(size)
            .toList()
    }

    override suspend fun getPage(link: String): String {
        return webClient.get()
            .uri(link)
            .accept(MediaType.TEXT_HTML)
            .retrieve()
            .awaitBody<String>()
    }
}
