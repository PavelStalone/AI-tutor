package rut.uvp.deepsearch.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Configuration
internal class SearchConfig {

    private val httpClient = HttpClient.create()
        .followRedirect(true)

    @Bean
    fun provideWebClient(): WebClient = WebClient.builder()
        .codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
        .clientConnector(ReactorClientHttpConnector(httpClient))
        .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; DeepSearchBot/1.0)")
        .build()
}
