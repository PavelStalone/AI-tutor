package rut.uvp.feature.deepsearch.infrastructure.model

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder("jobTitle", "jobDescription", "candidateRequirements", "workingConditions", "location", "contactInfo")
internal data class VacancyResponse(
    val jobTitle: String,
    val jobDescription: String,
    val candidateRequirements: String,
    val workingConditions: String,
    val location: String,
    val contactInfo: String,
)
