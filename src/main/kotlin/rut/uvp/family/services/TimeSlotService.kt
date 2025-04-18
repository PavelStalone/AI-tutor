package rut.uvp.family.services

import org.springframework.stereotype.Service
import rut.uvp.family.models.SelectedTimeSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Сервис для работы с временными слотами
 */
@Service
class TimeSlotService {
    
    // Форматтер для дат
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Генерирует список временных слотов для указанной даты
     */
    fun generateTimeSlotsForDate(date: LocalDate): List<SelectedTimeSlot> {
        val formattedDate = date.format(dateFormatter)
        
        return listOf(
            SelectedTimeSlot(formattedDate, "10:00 - 12:00"),
            SelectedTimeSlot(formattedDate, "12:00 - 14:00"),
            SelectedTimeSlot(formattedDate, "14:00 - 16:00"),
            SelectedTimeSlot(formattedDate, "16:00 - 18:00"),
            SelectedTimeSlot(formattedDate, "18:00 - 20:00")
        )
    }
} 