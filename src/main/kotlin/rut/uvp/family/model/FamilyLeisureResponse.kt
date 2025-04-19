package rut.uvp.family.model

import rut.uvp.family.service.KudaGoEvent

data class FamilyLeisureResponse(
    val id: Long? = null,
    val title: String,
    val imageUrl: String?,
    val date: String?,
    val time: String?,
    val location: String?,
    val address: String?,
    val lat: Double?,
    val lon: Double?,
    val description: String?,
    val familyId: Long? = null
) {
    companion object {
        fun fromKudaGoEvent(event: KudaGoEvent, familyId: Long? = null): FamilyLeisureResponse {
            val firstDate = event.dates?.firstOrNull()
            val firstImage = event.images?.firstOrNull()
            val coords = event.place?.coords
            return FamilyLeisureResponse(
                id = event.id,
                title = event.title ?: "",
                imageUrl = firstImage?.image,
                date = firstDate?.start_date,
                time = firstDate?.start_time,
                location = event.place?.title,
                address = event.place?.address,
                lat = coords?.lat,
                lon = coords?.lon,
                description = event.description,
                familyId = familyId
            )
        }
    }
}
