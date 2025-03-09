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
    fun runner(chatClient: ChatClient, testTools: WorkTools) = CommandLineRunner { _ ->
        val testQuestion =
            "Составь список вакансий, которые мне подходят. Я знаю Java, Python на среднем уровне, Git, изучал HTML и CSS"
        println("Send question: $testQuestion")

        chatClient
            .prompt(testQuestion)
            .tools(
                testTools
            )
            .stream()
            .content().subscribe {
                print(it)
            }

        println("Finish")
    }
}

fun main(args: Array<String>) {
    runApplication<FamilyApplication>(*args)
}
