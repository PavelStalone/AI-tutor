package rut.uvp.family.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import rut.uvp.family.service.FamilyService
import rut.uvp.family.service.FamilyMemberService
import rut.uvp.family.service.UserService

@RestController
@RequestMapping("/family")
class FamilyController(
    private val familyService: FamilyService,
    private val familyMemberService: FamilyMemberService,
    private val userService: UserService
) {
    data class CreateFamilyRequest(val creatorId: Long)
    @PostMapping("/create")
    fun createFamily(@RequestBody req: CreateFamilyRequest): ResponseEntity<Any> {
        val family = familyService.createFamily(req.creatorId)
        return ResponseEntity.ok(mapOf("familyCode" to family.familyCode, "familyId" to family.id))
    }

    data class JoinFamilyRequest(val userId: Long, val familyCode: String, val relation: String)
    @PostMapping("/join")
    fun joinFamily(@RequestBody req: JoinFamilyRequest): ResponseEntity<Any> {
        val family = familyService.findByCode(req.familyCode) ?: return ResponseEntity.badRequest().body("Invalid code")
        familyMemberService.addMember(req.userId, family.id, req.relation)
        return ResponseEntity.ok(mapOf("familyId" to family.id))
    }

    @GetMapping("/{id}/members")
    fun getMembers(@PathVariable id: Long): ResponseEntity<Any> {
        val members = familyMemberService.findByFamily(id)
        return ResponseEntity.ok(members)
    }
} 