package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.Family
import rut.uvp.family.repository.FamilyRepository
import java.util.*

@Service
class FamilyService(
    private val familyRepository: FamilyRepository
) {
    fun createFamily(creatorId: Long): Family {
        val code = generateFamilyCode()
        val family = Family(familyCode = code, creatorId = creatorId)
        return familyRepository.save(family)
    }

    fun findByCode(code: String): Family? = familyRepository.findByFamilyCode(code)
    fun findById(id: Long): Family? = familyRepository.findById(id).orElse(null)

    private fun generateFamilyCode(): String = UUID.randomUUID().toString().substring(0, 8)
} 