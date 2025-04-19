package rut.uvp.family.model

import jakarta.persistence.*

@Entity
@Table(name = "family_members")
data class FamilyMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    val userId: Long = 0,
    @Column(nullable = false)
    val familyId: Long = 0,
    @Column(nullable = false)
    val relation: String = "",
    @Column(nullable = true)
    val interests: String? = null
)