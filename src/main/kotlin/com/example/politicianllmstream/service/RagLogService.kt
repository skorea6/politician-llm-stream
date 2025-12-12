package com.example.politicianllmstream.service

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class RagLogService(
    private val redis: ReactiveStringRedisTemplate
) {
    fun saveLog(ip: String, query: String): Mono<Boolean> {
        val key = "llm-stream:log:${System.currentTimeMillis()}:${ip}"

        return redis.opsForValue()
            .set(key, query, Duration.ofDays(15)) // TTL 15일
    }
}