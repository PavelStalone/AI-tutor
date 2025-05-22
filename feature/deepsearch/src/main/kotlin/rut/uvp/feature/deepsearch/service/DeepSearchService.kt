package rut.uvp.deepsearch.service

import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import rut.uvp.core.ai.config.ChatClientQualifier
import rut.uvp.core.common.log.Log
import rut.uvp.deepsearch.domain.model.Activity
import rut.uvp.deepsearch.domain.repository.SearchRepository
import rut.uvp.deepsearch.infrastructure.model.ActivityResponse
import java.net.URLDecoder
import kotlin.time.Duration.Companion.seconds

interface DeepSearchService {

    suspend fun deepSearch(query: String): List<Activity>
}

@Service
internal class DeepSearchServiceImpl(
    @Qualifier(ChatClientQualifier.ACTIVITY_FINDER_CLIENT)
    private val chatClient: ChatClient,
    private val searchRepository: SearchRepository,
) : DeepSearchService {

    override suspend fun deepSearch(query: String): List<Activity> = withContext(Dispatchers.IO) {
        val clearQuery = query.trim().trimEnd('.').removeSurrounding("\"")
        Log.i("DeepSearch started for query: $clearQuery")

        val links = searchRepository.getLinks(query = clearQuery, size = 3)
        Log.i("DeepSearch links: $links")

        coroutineScope {
            links
                .map { link -> link.decode() }
                .map { link ->
                    async(start = CoroutineStart.LAZY) { // TODO: Add batching strategy - shoplikpavel
                        runCatching {
                            Log.d("Start parsing: $link")

                            Pair(
                                first = link,
                                second = withTimeout(5.seconds) {
                                    Jsoup.parse(searchRepository.getPage(link)).body().text()
                                }
                            )
                        }.onFailure { throwable ->
                            Log.e(throwable, "Failure link: $link")
                        }.getOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
                .map { (link, page) ->
                    async(start = CoroutineStart.LAZY) {
                        runCatching {
                            Log.i("Start finds activity: $link")

                            chatClient
                                .prompt(page)
                                .call()
                                .entity(object : ParameterizedTypeReference<List<ActivityResponse>>() {})
                                ?.map { response ->
                                    Activity(
                                        title = response.title,
                                        description = response.description,
                                        dateRange = response.dateRange,
                                        location = response.location,
                                        url = link
                                    )
                                }
                        }.getOrNull()
                    }
                }
                .awaitAll()
                .filterNotNull()
                .flatten()
        }
    }

    private fun String.decode(): String = runCatching {
        val query = this
        require(query.contains("uddg="))

        val encodedUddg = query.substringAfter("uddg=").substringBefore("&")
        val firstDecode = URLDecoder.decode(encodedUddg, "UTF-8")
        val realUrl = URLDecoder.decode(firstDecode, "UTF-8")

        realUrl
    }.onFailure { throwable ->
        Log.e(throwable, "error decoding")
    }
        .getOrDefault(this)
}
