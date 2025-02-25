package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Description
import java.util.function.Function

@SpringBootApplication
class FamilyApplication {

    @Bean
    fun runner(chatClient: ChatClient) = CommandLineRunner { _ ->
        println("Start question")

        chatClient
            .prompt("Какая температура в Москве?")
            .tools(WeatherTools())
            .call()
            .content().also {
                println(it)
            }
    }
}

fun main(args: Array<String>) {
    runApplication<FamilyApplication>(*args)
}
