package rut.uvp.family.model

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true, nullable = false)
    val email: String,
    @Column(nullable = false)
    val passwordHash: String,
    @Column(nullable = false)
    val name: String
) 