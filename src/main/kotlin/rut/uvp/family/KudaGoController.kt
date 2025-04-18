package rut.uvp.family

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import rut.uvp.family.models.ActivityRecommendation
import rut.uvp.family.models.KudaGoCategory
import rut.uvp.family.models.KudaGoEvent
import rut.uvp.family.models.KudaGoLocation
import rut.uvp.family.models.KudaGoSearchParams
import rut.uvp.family.services.KudaGoApiService
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/kudago")
class KudaGoController(
    private val kudaGoApiService: KudaGoApiService
) {

    @GetMapping("/locations")
    fun getLocations(): ResponseEntity<List<KudaGoLocation>> {
        val locations = kudaGoApiService.getLocations()
        return ResponseEntity.ok(locations)
    }

    @GetMapping("/categories")
    fun getCategories(): ResponseEntity<List<KudaGoCategory>> {
        val categories = kudaGoApiService.getCategories()
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/events")
    fun searchEvents(
        @RequestParam(required = false, defaultValue = "msk") location: String,
        @RequestParam(required = false) categories: List<String>?,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) isFree: Boolean?,
        @RequestParam(required = false) ageRestriction: String?,
        @RequestParam(required = false, defaultValue = "30") daysAhead: Int
    ): ResponseEntity<List<ActivityRecommendation>> {
        val now = Instant.now()
        val endDate = now.plus(daysAhead.toLong(), ChronoUnit.DAYS)
        
        val params = KudaGoSearchParams(
            location = location,
            categories = categories ?: emptyList(),
            actualSince = now,
            actualUntil = endDate,
            ageRestriction = ageRestriction,
            isFree = isFree,
            query = query,
            pageSize = 20
        )
        
        val events = kudaGoApiService.searchEvents(params)
        
        val recommendations = kudaGoApiService.convertToRecommendations(events)
        
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/events/{id}")
    fun getEventDetails(@PathVariable id: Int): ResponseEntity<KudaGoEvent> {
        val event = kudaGoApiService.getEventDetails(id)
        
        return if (event != null) {
            ResponseEntity.ok(event)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
