package rut.uvp.family.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import rut.uvp.family.service.UserService

@RestController
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService
) {
    data class RegisterRequest(val email: String, val password: String, val name: String)
    @PostMapping("/register")
    fun register(@RequestBody req: RegisterRequest): ResponseEntity<Any> {
        val user = userService.register(req.email, req.password, req.name)
        return ResponseEntity.ok(mapOf("userId" to user.id))
    }
} 