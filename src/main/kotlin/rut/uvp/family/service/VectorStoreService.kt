package rut.uvp.family.service

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import rut.uvp.family.log.Log

@Service
class VectorStoreService(
    private val vectorStore: QdrantVectorStore,
) {

    val currentTime
        get() = Clock.System.now().toEpochMilliseconds()

    fun addWithTTL(text: String, instant: Instant) {
        val metadata = mapOf(TIMESTAMP_PROPERTY to instant.toEpochMilliseconds())
        val document = Document(text, metadata)

        vectorStore.add(listOf(document))
    }

    fun search(query: SearchRequest.Builder.() -> Unit): List<Document> {
        Log.v("search called")

        val defaultFilter = "$TIMESTAMP_PROPERTY > $currentTime OR exists($TIMESTAMP_PROPERTY) == false"

        return vectorStore
            .similaritySearch(
                SearchRequest.builder()
                    .apply(query)
                    .filterExpression(defaultFilter)
                    .build()
            )
            ?.toList()
            ?: emptyList()
    }

    @Scheduled(fixedRate = 60 * 60 * 1000) // Every 1 hour | Check analog "HandFier"
    private fun removeOldData() {
        Log.d("Start clear old data")

        vectorStore.delete("$TIMESTAMP_PROPERTY <= $currentTime")
    }

    companion object {
        private const val TIMESTAMP_PROPERTY = "timestamp"
    }
}
