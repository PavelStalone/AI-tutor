package rut.uvp.family.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "calendar_events")
data class CalendarEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val userId: Long,
    @Column(nullable = false)
    val familyId: Long,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false)
    val start: LocalDateTime,
    @Column(name = "end_time", nullable = false)
    val end: LocalDateTime,
    @Column(nullable = false)
    val source: String // google, apple, etc
) 