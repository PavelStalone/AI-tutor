package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order


@SpringBootApplication
class FamilyApplication {

    @Bean
    @Order(2)
    fun runner(chatClient: ChatClient) = CommandLineRunner { _ ->
        val testQuestion = "Куда можно устроиться зная Java и немного Git?"
        println("Send question: $testQuestion")

        chatClient
            .prompt(testQuestion)
            .tools(TestTools())
            .call()
            .content().also {
                println(it)
            }

        println("Finish")
    }
}

fun main(args: Array<String>) {
    runApplication<FamilyApplication>(*args)
}
