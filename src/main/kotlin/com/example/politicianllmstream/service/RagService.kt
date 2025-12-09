package com.example.politicianllmstream.service

import com.example.politicianllmstream.dto.RagRequest
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Service
class RagService(
    private val ragWebClient: WebClient
) {
    fun askStream(payload: RagRequest): Flux<String> {
        return ragWebClient.post()
            .bodyValue(payload)
            .retrieve()
            .bodyToFlux(String::class.java)
    }
}
