package com.example.politicianllmstream.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    @Value("\${ai.url}") private val aiUrl: String,
    @Value("\${ai.secret-key}") private val secretKey: String
) {

    @Bean
    fun ragWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(aiUrl)
            .defaultHeader("X-AI-Key", secretKey)
            .build()
    }
}