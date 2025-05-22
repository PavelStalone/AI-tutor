package rut.uvp.feature.resume.service

import kotlinx.datetime.*
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import rut.uvp.core.common.log.Log
import kotlin.time.Duration.Companion.days

@Service
class VacancyStoreService(
    @Qualifier("VacancyStore") private val vectorStore: VectorStore,
) {

    private val formatter = DateTimeComponents.Format {
        date(LocalDate.Formats.ISO)
        alternativeParsing({ char('t') }) { char('T') }

        hour(); char(':'); minute(); char(':'); second()

        alternativeParsing({ offsetHours() }) { offset(UtcOffset.Formats.ISO) }
    }

    val currentTime
        get() = Clock.System.now()

    fun addWithTTL(text: String, instant: Instant) {
        val timestamp = instant.format(format = formatter)

        Log.v("addWithTTL called. text: $text, timestamp: $timestamp")
        val metadata = mapOf(TIMESTAMP_PROPERTY to instant.toEpochMilliseconds().toInt())
        val document = Document(text, metadata)

        vectorStore.add(listOf(document))
    }

    fun search(searchRequest: SearchRequest): List<Document> {
        Log.v("search called: $searchRequest")

        val b = FilterExpressionBuilder()
        val defaultFilter = b.or(
            b.gt(TIMESTAMP_PROPERTY, currentTime.toEpochMilliseconds().toInt()),
            b.eq(TIMESTAMP_PROPERTY, 0)
        ).build()

        return vectorStore
            .similaritySearch(
                SearchRequest
                    .from(searchRequest)
                    .filterExpression(defaultFilter)
                    .build()
                    .also { Log.v("Final search: $it") }
            )
            ?.toList()
            ?: emptyList()
    }

    fun delete(idList: List<String>) {
        vectorStore.delete(idList)
    }

    @Scheduled(fixedRate = 60 * 60 * 1000) // Every 1 hour | Check analog "HandFier"
    private fun removeOldData() {
        Log.d("Clear old data")

        val b = FilterExpressionBuilder()
        val defaultFilter = b.lte(TIMESTAMP_PROPERTY, currentTime.toEpochMilliseconds().toInt()).build()

        vectorStore.delete(defaultFilter)
    }

    companion object {

        private const val TIMESTAMP_PROPERTY = "timestamp"
    }
}
