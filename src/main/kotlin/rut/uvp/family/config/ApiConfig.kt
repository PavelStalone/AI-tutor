package rut.uvp.family.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate

/**
 * Конфигурация API клиентов и кэширования
 */
@Configuration
@EnableCaching
@EnableScheduling
class ApiConfig {
    
    /**
     * Настройка RestTemplate для API запросов
     * 
     * @return Настроенный RestTemplate
     */
    @Bean
    fun restTemplate(): RestTemplate {
        val factory = SimpleClientHttpRequestFactory()
        factory.setConnectTimeout(5000) // 5 секунд таймаут на подключение
        factory.setReadTimeout(15000) // 15 секунд таймаут на чтение
        
        // Дополнительные настройки для HttpURLConnection
        factory.setOutputStreaming(false)
        
        return RestTemplate(factory)
    }
    
    /**
     * Настройка менеджера кэшей
     * 
     * @return Менеджер кэшей
     */
    @Bean
    fun cacheManager(): CacheManager {
        return ConcurrentMapCacheManager(
            "kudago-locations",
            "kudago-categories",
            "kudago-events",
            "kudago-event-details"
        )
    }
} 