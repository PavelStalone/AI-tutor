package rut.uvp.family.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import rut.uvp.family.model.User
import rut.uvp.family.repository.UserRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(email: String, password: String, name: String): User {
        require(userRepository.findByEmail(email) == null) { "Email already registered" }
        val user = User(
            email = email,
            passwordHash = passwordEncoder.encode(password),
            name = name
        )
        return userRepository.save(user)
    }

    fun findByEmail(email: String): User? = userRepository.findByEmail(email)
    fun findById(id: Long): User? = userRepository.findById(id).orElse(null)
} 