package rut.uvp.family.model

import jakarta.persistence.*

@Entity
@Table(name = "families")
data class Family(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true, nullable = false)
    val familyCode: String = "",
    @Column(nullable = false)
    val creatorId: Long = 0
) 