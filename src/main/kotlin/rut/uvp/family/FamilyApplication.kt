package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.SimpleVectorStore
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@SpringBootApplication
@EnableCaching
class FamilyApplication {

//    @Bean
//    @Order(2)
//    fun runner(chatClient: ChatClient, testTools: WorkTools, embeddingModel: EmbeddingModel) = CommandLineRunner { _ ->
//        val testQuestion = "Составь список вакансий, которые мне подходя, основываясь на моем стеке"
//        println("Send question: $testQuestion")
//
//        val resumeStore = UserStoreImpl(
//            userId = "user",
//            textSplitter = TokenTextSplitter(
//                500,
//                200,
//                10,
//                5000,
//                false
//            ),
//            vectorStoreBuilder = SimpleVectorStore.builder(embeddingModel)
//        )
//
//        resumeStore.saveResume("test_resume", "Мой стек технологий: Java, Python на среднем уровне, Git, HTML и CSS")
//
//        chatClient
//            .prompt(testQuestion)
//            .apply {
//                resumeStore.getResume("test_resume")?.let { vectorStore ->
//                    advisors(QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().topK(10).build()))
//                }
//            }
//            .tools(testTools)
//            .stream()
//            .content()
//            .subscribe(::print)
//
//        println("Finish")
//    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

@Configuration
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}

fun main(args: Array<String>) {
    runApplication<FamilyApplication>(*args)
}
