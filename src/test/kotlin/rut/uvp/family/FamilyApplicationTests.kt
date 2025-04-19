package rut.uvp.family

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.junit.jupiter.api.Assertions.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FamilyApplicationTests {

	@Autowired
	lateinit var restTemplate: TestRestTemplate

	@Test
	fun contextLoads() {
	}

	@Test
	fun testLeisureFlow() {
		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_JSON
		val body = "{\"message\": \"Что поделать с дочкой 6 лет в эти выходные в Москве?\"}"
		val entity = HttpEntity(body, headers)
		val response: ResponseEntity<String> = restTemplate.postForEntity("/chat", entity, String::class.java)
		assertTrue(response.body?.contains("title") == true)
	}

}
