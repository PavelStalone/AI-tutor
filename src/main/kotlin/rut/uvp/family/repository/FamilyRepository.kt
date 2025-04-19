package rut.uvp.family.repository

import org.springframework.data.jpa.repository.JpaRepository
import rut.uvp.family.model.Family

interface FamilyRepository : JpaRepository<Family, Long> {
    fun findByFamilyCode(familyCode: String): Family?
} 