package rut.uvp.feature.deepsearch.domain.model

data class Vacancy(
    val jobTitle: String,
    val jobDescription: String,
    val candidateRequirements: String,
    val workingConditions: String,
    val location: String,
    val contactInfo: String,
    val url: String,
)
