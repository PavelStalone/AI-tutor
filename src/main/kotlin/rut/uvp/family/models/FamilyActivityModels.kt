package rut.uvp.family.models

// Request model to capture family activity request information
data class ActivityRequestData(
    val familyMember: FamilyMemberData? = null,
    val date: String? = null,
    val dayOfWeek: String? = null,
    val preferences: List<String> = emptyList(),
    val restrictions: List<String> = emptyList(),
    val needsTimeSlotSelection: Boolean = false,
    val activityType: String? = null,
    val preferredDate: String? = null,
    val budgetConstraint: String? = null,
    val locationPreference: String? = null,
    val activityDuration: String? = null,
    val specialRequirements: List<String> = emptyList()
)

// Family member information
data class FamilyMemberData(
    val role: String? = null, // e.g., daughter, son, wife, etc.
    val age: Int? = null
)

// Selected date and time slot
data class SelectedTimeSlot(
    val selectedDate: String,
    val selectedTimeRange: String
)

// Search query for activities
data class ActivitySearchQuery(
    val searchQuery: String,
    val filters: Map<String, String> = emptyMap()
)

// Activity recommendation from parser
data class ActivityRecommendation(
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val price: String? = null,
    val ageRestriction: String? = null,
    val category: String? = null,
    val url: String? = null
)

// Complete response with all recommendations
data class FamilyActivityResponse(
    val request: ActivityRequestData,
    val selectedTimeSlot: SelectedTimeSlot? = null,
    val activities: List<ActivityRecommendation> = emptyList()
) 