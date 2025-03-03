package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class FamilyApplication {

    @Bean
    fun runner(chatClient: ChatClient) = CommandLineRunner { _ ->
        val testQuestion = "Я знаю Java и немного Git. Запиши меня на собеседование, там где я смогу устроиться."
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
