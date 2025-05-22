package rut.uvp.deepsearch.infrastructure.model

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("title", "description", "dateRange", "location")
internal data class ActivityResponse(
    val title: String,
    val description: String,
    val dateRange: String,
    val location: String,
)
