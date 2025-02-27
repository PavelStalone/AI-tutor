package rut.uvp.family

import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication(
//    exclude = [MongoAutoConfiguration::class, MongoDataAutoConfiguration::class]
)
class FamilyApplication {

    @Bean
    fun runner(chatClient: ChatClient) = CommandLineRunner { _ ->
        println("Start question")

        chatClient
            .prompt("Я знаю Java и немного Git, где и кем я могу работать?")
            .tools(WeatherTools())
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
