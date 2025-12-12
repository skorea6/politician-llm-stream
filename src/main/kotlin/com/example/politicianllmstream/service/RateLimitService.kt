package com.example.politicianllmstream.service

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class RateLimitService(
    private val redis: ReactiveStringRedisTemplate
) {
    private val limit = 10       // 1시간 10회
    private val ttl = Duration.ofHours(1)

    fun check(ip: String): Mono<Boolean> {
        val key = "llm-stream:ip:$ip:limit"

        return redis.opsForValue().increment(key)
            .flatMap { count ->
                if (count == 1L) {
                    // 첫 요청이면 TTL 설정
                    redis.expire(key, ttl).thenReturn(count)
                } else Mono.just(count)
            }.map { count ->
                count <= limit
            }
    }
}