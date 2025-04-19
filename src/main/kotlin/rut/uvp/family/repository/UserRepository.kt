package rut.uvp.family.repository

import org.springframework.data.jpa.repository.JpaRepository
import rut.uvp.family.model.User

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
} 