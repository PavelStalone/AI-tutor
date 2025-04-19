package rut.uvp.family.service

import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.DayOfWeek

@Service
class DateSelectionService {
    fun selectDate(members: List<String>): Pair<String, String> {
        // TODO: интеграция с календарями и RAG
        // Пока что: ближайшая суббота, 12:00-15:00
        val today = LocalDate.now()
        val nextSaturday = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        return nextSaturday.toString() to "12:00-15:00"
    }
}