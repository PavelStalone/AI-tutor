package rut.uvp.family.service

import org.springframework.stereotype.Service
import rut.uvp.family.model.*
import rut.uvp.family.repository.CalendarEventRepository
import rut.uvp.family.repository.FamilyMemberRepository
import rut.uvp.family.repository.UserRepository

@Service
class RagDataService(
    private val familyMemberRepository: FamilyMemberRepository,
    private val userRepository: UserRepository,
    private val calendarEventRepository: CalendarEventRepository
) {
    fun getFamilyContext(familyId: Long): FamilyContext {
        val members = familyMemberRepository.findByFamilyId(familyId)
            .map { member ->
                val user = userRepository.findById(member.userId).orElse(null)
                FamilyContext.Member(
                    userId = member.userId,
                    name = user?.name ?: "",
                    relation = member.relation
                )
            }
        val events = calendarEventRepository.findByFamilyId(familyId)
        return FamilyContext(members, events)
    }
}

data class FamilyContext(
    val members: List<Member>,
    val events: List<CalendarEvent>
) {
    data class Member(val userId: Long, val name: String, val relation: String)
} 