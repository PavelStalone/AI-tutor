package rut.uvp.family.repository

import org.springframework.data.jpa.repository.JpaRepository
import rut.uvp.family.model.FamilyMember

interface FamilyMemberRepository : JpaRepository<FamilyMember, Long> {
    fun findByFamilyId(familyId: Long): List<FamilyMember>
    fun findByUserId(userId: Long): List<FamilyMember>
} 