package rut.uvp.family.models

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Модель для члена семьи
 */
data class FamilyMember(
    val id: String, // Уникальный идентификатор члена семьи
    val name: String, // Имя
    val birthDate: LocalDate, // Дата рождения
    val gender: Gender, // Пол
    val preferences: List<String> = emptyList(), // Предпочтения по активностям
    val restrictions: List<String> = emptyList(), // Ограничения
    val isAccountOwner: Boolean = false, // Является ли владельцем аккаунта
    val relationToOwner: RelationToOwner? = null, // Кем приходится владельцу аккаунта
    val createdAt: LocalDateTime = LocalDateTime.now(), // Дата создания записи
    val updatedAt: LocalDateTime = LocalDateTime.now() // Дата обновления записи
)

/**
 * Модель для хранения событий календаря
 */
data class CalendarEvent(
    val id: String, // Уникальный идентификатор события
    val familyMemberId: String, // ID члена семьи, к которому относится событие
    val title: String, // Название события
    val description: String? = null, // Описание события
    val startDateTime: LocalDateTime, // Дата и время начала
    val endDateTime: LocalDateTime, // Дата и время окончания
    val location: String? = null, // Место проведения
    val isRecurring: Boolean = false, // Повторяющееся событие
    val recurrenceRule: String? = null, // Правило повторения (например, "WEEKLY")
    val isAllDay: Boolean = false, // Событие на весь день
    val createdAt: LocalDateTime = LocalDateTime.now(), // Дата создания записи
    val updatedAt: LocalDateTime = LocalDateTime.now() // Дата обновления записи
)

/**
 * Перечисление для пола
 */
enum class Gender {
    MALE, FEMALE, OTHER
}

/**
 * Перечисление для отношения к владельцу аккаунта
 */
enum class RelationToOwner {
    SPOUSE, // Супруг(а)
    SON, // Сын
    DAUGHTER, // Дочь
    FATHER, // Отец
    MOTHER, // Мать
    BROTHER, // Брат
    SISTER, // Сестра
    GRANDFATHER, // Дедушка
    GRANDMOTHER, // Бабушка
    OTHER // Другое
} 