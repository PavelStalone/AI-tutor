package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.FamilyMember
import rut.uvp.family.repository.FamilyMemberRepository

@Service
class FamilyMemberService(
    private val familyMemberRepository: FamilyMemberRepository
) {
    fun addMember(userId: Long, familyId: Long, relation: String, interests: String? = null): FamilyMember {
        val member = FamilyMember(userId = userId, familyId = familyId, relation = relation, interests = interests)
        return familyMemberRepository.save(member)
    }
    fun findByFamily(familyId: Long): List<FamilyMember> = familyMemberRepository.findByFamilyId(familyId)
    fun findByUser(userId: Long): List<FamilyMember> = familyMemberRepository.findByUserId(userId)
} 