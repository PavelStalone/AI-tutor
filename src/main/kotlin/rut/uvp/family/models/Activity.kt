package rut.uvp.family.models

/**
 * Модель для представления активности или мероприятия
 */
data class Activity(
    val id: String = "",
    val title: String,
    val description: String = "",
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val price: String? = null,
    val ageRestriction: String? = null,
    val category: String? = null,
    val link: String? = null,
    val imageUrl: String? = null
) 