package rut.uvp.family

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import rut.uvp.family.models.FamilyActivityResponse
import kotlin.test.assertNotNull

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FamilyActivityTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should process family activity request`() {
        // Given
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        
        val requestBody = """
            {
                "message": "Что поделать с дочкой 6 лет в эти выходные?"
            }
        """.trimIndent()
        
        val request = HttpEntity(requestBody, headers)
        
        // When
        val response = restTemplate.postForObject(
            "/family-activity",
            request,
            FamilyActivityResponse::class.java
        )
        
        // Then
        assertNotNull(response)
        assertNotNull(response.request)
        assertNotNull(response.request.familyMember)
        println("Response: $response")
    }
} 